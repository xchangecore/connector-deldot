package com.saic.uicds.clients.em.deldotAdapter;

import gov.niem.niem.niemCore.x20.ActivityDateDocument;
import gov.niem.niem.niemCore.x20.CircularRegionType;
import gov.niem.niem.niemCore.x20.DateTimeDocument;
import gov.niem.niem.niemCore.x20.DateType;

import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.uicds.incident.IncidentDocument;
import org.uicds.incident.UICDSIncidentType;

import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.uicds.clients.util.Common;

public class WorkProductContentEnricher {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String DELDOT = "DELDOT";
    public static final String DELDOT_RTTA = "RTTA";
    public static final String DELDOT_CREATED_REASON = "CREATED";
    private static final String idXPath = "id";
    private static final String typeXPath = "type";
    private static final String countyXPath = "county";
    private static final String detailsXPath = "details";
    private static final String latitudeXPath = "latitude";
    private static final String longitudeXPath = "longitude";

    @Transformer
    public Message<WorkProductDocument> createWorkProduct(Message<IncidentDocumentMessage> message) {

        // Find the current incident document if it exists in the message headers
        WorkProductDocument workProductDoc = message.getPayload().getWorkProductDocument();

        // Create a new work product doc if one was not passed through the headers
        // this indicates that the incident for this item doesn't exist.
        if (workProductDoc == null) {
            workProductDoc = WorkProductDocument.Factory.newInstance();
        }

        if (message.getPayload().getRtta() != null) {
            workProductDoc = getCreateOrUpdateWorkProduct(message, workProductDoc);
            return new GenericMessage<WorkProductDocument>(workProductDoc);
        } else {
            return MessageBuilder.withPayload(message.getPayload().getWorkProductDocument()).setHeaderIfAbsent(
                "ARCHIVE", true).build();

        }
    }

    public WorkProductDocument getCreateOrUpdateWorkProduct(
        Message<IncidentDocumentMessage> message, WorkProductDocument workProductDoc) {

        IncidentDocument incident = createIncidentFromRtta(message.getPayload().getRtta());

        if (workProductDoc.getWorkProduct() == null) {
            workProductDoc.addNewWorkProduct();
        }
        if (workProductDoc.getWorkProduct().sizeOfStructuredPayloadArray() == 0) {
            workProductDoc.getWorkProduct().addNewStructuredPayload();
        }
        workProductDoc.getWorkProduct().getStructuredPayloadArray(0).set(incident);

        return workProductDoc;
    }

    private IncidentDocument createIncidentFromRtta(XmlObject payload) {

        IncidentDocument incident = IncidentDocument.Factory.newInstance();
        incident.addNewIncident();

        setDeldotID(incident.getIncident(), payload);
        setActivityCategory(incident.getIncident(), payload);
        setActivityName(incident.getIncident(), payload);
        setActivityDescription(incident.getIncident(), payload);
        setIncidentLocation(incident.getIncident(), payload);
        setActivityDate(incident.getIncident(), payload);

        return incident;
    }

    private void setActivityDate(UICDSIncidentType incident, XmlObject payload) {

        String dateString = DeldotUtils.getDateStringFromRTTA(payload);

        if (dateString == null) {
            dateString = DeldotUtils.getNowAsString();
        }

        DateTimeDocument dateDoc = DateTimeDocument.Factory.newInstance();
        dateDoc.addNewDateTime().setStringValue(dateString);

        ActivityDateDocument activityDate = ActivityDateDocument.Factory.newInstance();
        activityDate.addNewActivityDate().set(dateDoc);

        if (incident.sizeOfActivityDateRepresentationArray() < 1) {
            Common.substitute(incident.addNewActivityDateRepresentation(), Common.NIEM_NS,
                Common.ACTIVITY_DATE, DateType.type, activityDate.getActivityDate());
        } else {
            incident.getActivityDateRepresentationArray(0).set(activityDate.getActivityDate());
        }
    }

    private void setIncidentLocation(UICDSIncidentType incident, XmlObject payload) {

        XmlObject[] latitudes = payload.selectPath(latitudeXPath);
        XmlObject[] longitudes = payload.selectPath(longitudeXPath);

        String latitude = null;
        String longitude = null;

        if (latitudes.length > 0) {
            latitude = Common.getTextFromAny(latitudes[0]);
        }
        if (longitudes.length > 0) {
            longitude = Common.getTextFromAny(longitudes[0]);
        }

        if (latitude == null)
            latitude = "0.0";
        if (longitude == null)
            longitude = "0.0";

        try {
            CircularRegionType circle = Common.createCircle(latitude, longitude);

            if (incident.sizeOfIncidentLocationArray() == 0) {
                incident.addNewIncidentLocation();
            }
            if (incident.getIncidentLocationArray(0).sizeOfLocationAreaArray() < 1) {
                incident.getIncidentLocationArray(0).addNewLocationArea();
            }
            if (incident.getIncidentLocationArray(0).getLocationAreaArray(0).sizeOfAreaCircularRegionArray() < 1) {
                incident.getIncidentLocationArray(0).getLocationAreaArray(0).addNewAreaCircularRegion();
            }

            incident.getIncidentLocationArray(0).getLocationAreaArray(0).getAreaCircularRegionArray(
                0).set(circle);

        } catch (StringIndexOutOfBoundsException e) {
            logger.error("Error creating circle location from (" + latitude + "," + longitude);
            logger.error("Latitude values: " + latitudes);
            logger.error("Longitude values: " + longitudes);
        }

    }

    private void setActivityDescription(UICDSIncidentType incident, XmlObject payload) {

        XmlObject[] objects = payload.selectPath(detailsXPath);
        if (objects.length > 0) {
            if (incident.sizeOfActivityDescriptionTextArray() == 0) {
                incident.addNewActivityDescriptionText();
            }
            incident.getActivityDescriptionTextArray(0).setStringValue(
                Common.getTextFromAny(objects[0]));
        }

    }

    private void setActivityName(UICDSIncidentType incident, XmlObject payload) {

        XmlObject[] county = payload.selectPath(countyXPath);
        XmlObject[] type = payload.selectPath(typeXPath);
        if (incident.sizeOfActivityNameArray() == 0) {
            incident.addNewActivityName();
        }
        String countyStr = null;
        String typeStr = null;
        if (county.length > 0) {
            countyStr = Common.getTextFromAny(county[0]);
        }
        if (type.length > 0) {
            typeStr = Common.getTextFromAny(type[0]);
        }
        if (countyStr == null || countyStr.isEmpty()) {
            countyStr = DELDOT;
        }
        if (typeStr == null || typeStr.isEmpty()) {
            typeStr = "Incident";
        }

        incident.getActivityNameArray(0).setStringValue(countyStr + "-" + typeStr);
    }

    private void setDeldotID(UICDSIncidentType incident, XmlObject payload) {

        XmlObject[] objects = payload.selectPath(idXPath);
        if (objects.length > 0) {
            if (!Common.activityStatusExists(incident, DELDOT, DELDOT_RTTA)) {
                Common.addIncidentEvent(incident, DELDOT_CREATED_REASON, DELDOT,
                    Common.getTextFromAny(objects[0]), DELDOT_RTTA);
            }
        }

    }

    private void setActivityCategory(UICDSIncidentType incident, XmlObject payload) {

        XmlObject[] objects = payload.selectPath(typeXPath);
        if (objects.length > 0) {
            if (incident.sizeOfActivityCategoryTextArray() == 0) {
                incident.addNewActivityCategoryText();
            }
            incident.getActivityCategoryTextArray(0).setStringValue(
                Common.getTextFromAny(objects[0]));
        }

    }
}
