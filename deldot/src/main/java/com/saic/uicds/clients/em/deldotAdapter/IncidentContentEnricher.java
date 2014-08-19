package com.saic.uicds.clients.em.deldotAdapter;

import gov.niem.niem.niemCore.x20.ActivityType;
import gov.niem.niem.niemCore.x20.TextType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.message.GenericMessage;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.incidentManagementService.GetIncidentListRequestDocument;
import org.uicds.incidentManagementService.GetIncidentListResponseDocument;
import org.uicds.workProductService.WorkProductListDocument.WorkProductList;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.em.async.UicdsCore;
import com.saic.uicds.clients.em.async.UicdsIncident;
import com.saic.uicds.clients.em.async.UicdsWorkProduct;
import com.saic.uicds.clients.util.Common;

public class IncidentContentEnricher {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private UicdsCore uicdsCore;

    HashMap<String, WorkProduct> currentDeldotIncidentWorkProducts;

    /**
     * @return the uicdsCore
     */
    public UicdsCore getUicdsCore() {

        return uicdsCore;
    }

    /**
     * @param uicdsCore the uicdsCore to set
     */
    public void setUicdsCore(UicdsCore uicdsCore) {

        this.uicdsCore = uicdsCore;
    }

    public IncidentContentEnricher() {

        currentDeldotIncidentWorkProducts = new HashMap<String, WorkProduct>();
    }

    @Transformer
    public Message<List<IncidentDocumentMessage>> enrichWithIncidentData(
        Message<List<IncidentDocumentMessage>> messages) {

        ArrayList<IncidentDocumentMessage> rttaMessages = new ArrayList<IncidentDocumentMessage>();

        // Get the current work products for all the incidents on the core
        updateCurrentIncidentWorkProducts();

        // Try to match each incoming message with an incident currently on the core
        for (IncidentDocumentMessage message : messages.getPayload()) {

            XmlObject rtta = message.getRtta();

            WorkProductDocument incidentWorkProduct = lookForExistingIncident(rtta);

            if (incidentWorkProduct == null) {
                rttaMessages.add(message);
            } else if (rttaItemNeedsUpdate(rtta, incidentWorkProduct)) {
                message.setWorkProductDocument(incidentWorkProduct);
                rttaMessages.add(message);
            }

        }

        // Find any incidents that are missing from the incoming list and setup to
        // close and archive the incident
        ArrayList<IncidentDocumentMessage> incidentsToArchive = getIncidentsToRemove(messages);
        if (incidentsToArchive.size() > 0) {
            rttaMessages.addAll(incidentsToArchive);
        }

        return new GenericMessage<List<IncidentDocumentMessage>>(rttaMessages);
    }

    private boolean rttaItemNeedsUpdate(XmlObject rtta, WorkProductDocument incidentWorkProduct) {

        IncidentDocument incident = Common.getIncidentDocumentFromWorkProduct(incidentWorkProduct.getWorkProduct());
        if (incident.getIncident().sizeOfActivityDateRepresentationArray() > 0) {
            SimpleDateFormat ISO8601Local = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            TimeZone timeZone = TimeZone.getDefault();
            ISO8601Local.setTimeZone(timeZone);

            Date date = Common.getISO8601LocalDateFromActivityDate(
                incident.getIncident().getActivityDateRepresentationArray(0), ISO8601Local);

            Date rttaDate = DeldotUtils.getDateFromRTTA(rtta);

            if (rttaDate.after(date)) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public ArrayList<IncidentDocumentMessage> getIncidentsToRemove(
        Message<List<IncidentDocumentMessage>> messages) {

        HashSet<String> set = new HashSet<String>();
        for (IncidentDocumentMessage message : messages.getPayload()) {
            XmlObject ids[] = message.getRtta().selectChildren("", "id");
            if (ids.length > 0) {
                set.add(Common.getTextFromAny(ids[0]));
            }
        }

        ArrayList<IncidentDocumentMessage> incidentsToArchive = new ArrayList<IncidentDocumentMessage>();
        for (String rttaID : currentDeldotIncidentWorkProducts.keySet()) {
            if (!set.contains(rttaID)) {
                IncidentDocumentMessage doc = new IncidentDocumentMessage();
                WorkProductDocument wpd = WorkProductDocument.Factory.newInstance();
                wpd.setWorkProduct(currentDeldotIncidentWorkProducts.get(rttaID));
                doc.setWorkProductDocument(wpd);
                incidentsToArchive.add(doc);
            }
        }
        return incidentsToArchive;
    }

    private void updateCurrentIncidentWorkProducts() {

        WorkProductList incidentList = getIncidentList();

        if (incidentList != null) {

            // only clear the list if we got a valid incident list
            currentDeldotIncidentWorkProducts.clear();

            for (WorkProduct wp : incidentList.getWorkProductArray()) {
                IdentificationType id = Common.getIdentificationElement(wp);
                IncidentDocument incidentDoc = getIncidentDocument(id);
                if (incidentDoc != null && incidentDoc.getIncident() != null) {
                    String deldotID = findDeldotID(incidentDoc.getIncident());
                    if (deldotID != null) {
                        if (wp.sizeOfStructuredPayloadArray() == 0) {
                            wp.addNewStructuredPayload();
                        }
                        wp.getStructuredPayloadArray(0).set(incidentDoc);
                        currentDeldotIncidentWorkProducts.put(deldotID, wp);
                    }
                }
            }

        }

    }

    public WorkProductDocument lookForExistingIncident(XmlObject rtta) {

        XmlObject[] ids = rtta.selectChildren("", "id");
        if (ids.length != 1) {
            logger.error("No ID found in the RTTA item");
            return null;
        }

        String rttaID = Common.getTextFromAny(ids[0]);
        if (rttaID == null) {
            logger.error("Unable to parse RTTA id from item");
            return null;
        }

        // Found an incident that was created from this DelDOT item
        if (currentDeldotIncidentWorkProducts.containsKey(rttaID)) {
            WorkProductDocument wpd = WorkProductDocument.Factory.newInstance();
            wpd.setWorkProduct(currentDeldotIncidentWorkProducts.get(rttaID));
            return wpd;
        }

        return null;
    }

    private IncidentDocument getIncidentDocument(IdentificationType workProductIdentifier) {

        WorkProduct workProduct = uicdsCore.getWorkProductFromCore(workProductIdentifier);
        if (workProduct != null) {
            UicdsWorkProduct uicdsWorkProduct = new UicdsWorkProduct(workProduct);
            XmlObject content = uicdsWorkProduct.getContent(UicdsIncident.INCIDENT_SERVICE_NS,
                UicdsIncident.INCIDENT_ELEMENT_NAME);
            if (content != null) {
                try {
                    IncidentDocument incident = IncidentDocument.Factory.parse(content.getDomNode());
                    return incident;
                } catch (XmlException e) {
                    logger.error("Error parsing Incident document");
                    return null;
                }
            }
        }
        return null;
    }

    public static String findDeldotID(UICDSIncidentType incident) {

        boolean foundDELDOT = false;
        boolean foundRECEIVED = false;

        if (incident != null && incident.sizeOfIncidentEventArray() > 0) {
            for (ActivityType event : incident.getIncidentEventArray()) {
                if (event.sizeOfActivityCategoryTextArray() > 0) {
                    for (TextType category : event.getActivityCategoryTextArray()) {
                        if (category.getStringValue().equalsIgnoreCase("DELDOT")) {
                            foundDELDOT = true;
                        }
                    }
                }
                if (event.sizeOfActivityReasonTextArray() > 0) {
                    for (TextType reason : event.getActivityReasonTextArray()) {
                        if (reason.getStringValue().equalsIgnoreCase("CREATED")) {
                            foundRECEIVED = true;
                        }
                    }
                }
                if (foundDELDOT && foundRECEIVED) {
                    if (event.sizeOfActivityIdentificationArray() > 0) {
                        for (gov.niem.niem.niemCore.x20.IdentificationType identification : event.getActivityIdentificationArray()) {
                            if (identification.sizeOfIdentificationCategoryDescriptionTextArray() > 0) {
                                if (identification.getIdentificationCategoryDescriptionTextArray(0).getStringValue().equalsIgnoreCase(
                                    "RTTA")) {
                                    if (identification.sizeOfIdentificationIDArray() > 0) {
                                        return identification.getIdentificationIDArray(0).getStringValue();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public WorkProductList getIncidentList() {

        GetIncidentListRequestDocument request = GetIncidentListRequestDocument.Factory.newInstance();
        request.addNewGetIncidentListRequest();
        XmlObject response = uicdsCore.marshalSendAndReceive(request);

        if (response instanceof GetIncidentListResponseDocument) {
            GetIncidentListResponseDocument incidentList = (GetIncidentListResponseDocument) response;
            if (incidentList != null) {
                return incidentList.getGetIncidentListResponse().getWorkProductList();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
