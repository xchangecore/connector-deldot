package com.saic.uicds.clients.em.deldotAdapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import gov.niem.niem.niemCore.x20.ActivityType;

import java.io.File;

import org.apache.xmlbeans.XmlObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.uicds.incident.IncidentDocument;

import x0.messageStructure1.PackageMetadataType;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.PropertiesType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.util.Common;
import com.saic.uicds.clients.util.CommonTestUtils;

public class WorkProductContentEnricherTest {

    private static final String INCIDENT_ID1 = "IG-1";

    WorkProductContentEnricher workProductContentEnricher;

    File inventoryFile;

    @Before
    public void init() {

        workProductContentEnricher = new WorkProductContentEnricher();
    }

    @Test
    public void testNewWorkProduct() {

        String fileName = "src/test/resources/rtta-1.xml";
        Message<IncidentDocumentMessage> request = createNewRttaRequest(fileName);

        Message<WorkProductDocument> response = workProductContentEnricher.createWorkProduct(request);

        checkResponse(response, true);
    }

    @Test
    public void testBadLocations() {

        String fileName = "src/test/resources/rtta-6.xml";
        Message<IncidentDocumentMessage> request = createNewRttaRequest(fileName);

        Message<WorkProductDocument> response = workProductContentEnricher.createWorkProduct(request);

        // checkResponse(response, true);
    }

    @Test
    public void testUpdateWorkProduct() {

        String fileName = "src/test/resources/rtta-1.xml";
        Message<IncidentDocumentMessage> request = createUpdateRttaRequest(fileName);

        Message<WorkProductDocument> response = workProductContentEnricher.createWorkProduct(request);

        checkResponse(response, false);
    }

    @Test
    public void testArchiveWorkProduct() {

        Message<IncidentDocumentMessage> request = createArchiveIncidentRequest();

        Message<WorkProductDocument> response = workProductContentEnricher.createWorkProduct(request);

        assertNotNull("null response", response);
        assertTrue("no archive header", response.getHeaders().containsKey("ARCHIVE"));
    }

    public void checkResponse(Message<WorkProductDocument> response, boolean isNew) {

        assertNotNull("response is null", response);

        WorkProduct wp = response.getPayload().getWorkProduct();
        assertNotNull("wp is null", wp);

        // new incidents have no id or props
        if (isNew) {
            assertNull("has wpid", Common.getIdentificationElement(wp));
            assertNull("has props", Common.getPropertiesElement(wp));
        } else {
            assertNotNull("no wpid", Common.getIdentificationElement(wp));
            assertNotNull("no props", Common.getPropertiesElement(wp));
        }

        assertEquals("no payloads", 1, wp.sizeOfStructuredPayloadArray());
        assertNotNull("no payload", wp);
        IncidentDocument incident = Common.getIncidentDocumentFromWorkProduct(wp);
        assertNotNull("no incident", incident);

        assertEquals("no types", 1, incident.getIncident().sizeOfActivityCategoryTextArray());
        assertEquals("wrong type", "Construction",
            incident.getIncident().getActivityCategoryTextArray(0).getStringValue());

        assertEquals("no date", 1, incident.getIncident().sizeOfActivityDateRepresentationArray());
        XmlObject date = incident.getIncident().getActivityDateRepresentationArray(0);
        String dateStr = Common.getTextFromAny(date);
        assertNotNull("null date", dateStr);
        assertFalse("no date text", dateStr.isEmpty());

        assertEquals("no events", 1, incident.getIncident().sizeOfIncidentEventArray());
        ActivityType events = incident.getIncident().getIncidentEventArray(0);
        assertEquals("no event category", 1, events.sizeOfActivityCategoryTextArray());
        assertEquals("wrong event category", WorkProductContentEnricher.DELDOT,
            events.getActivityCategoryTextArray(0).getStringValue());
        assertEquals("no reason", 1, events.sizeOfActivityReasonTextArray());
        assertEquals("wrong reason", WorkProductContentEnricher.DELDOT_CREATED_REASON,
            events.getActivityReasonTextArray(0).getStringValue());
        assertEquals("no id", 1, events.sizeOfActivityIdentificationArray());
        assertEquals("no id id", 1,
            events.getActivityIdentificationArray(0).sizeOfIdentificationIDArray());
        assertEquals("no id value", "32883",
            events.getActivityIdentificationArray(0).getIdentificationIDArray(0).getStringValue());
        assertEquals(
            "no id desc",
            1,
            events.getActivityIdentificationArray(0).sizeOfIdentificationCategoryDescriptionTextArray());
        assertEquals(
            "wrong id desc",
            WorkProductContentEnricher.DELDOT_RTTA,
            events.getActivityIdentificationArray(0).getIdentificationCategoryDescriptionTextArray(
                0).getStringValue());

        assertEquals("no location", 1, incident.getIncident().sizeOfIncidentLocationArray());
        assertEquals("no area", 1,
            incident.getIncident().getIncidentLocationArray(0).sizeOfLocationAreaArray());
        assertEquals(
            "no circle",
            1,
            incident.getIncident().getIncidentLocationArray(0).getLocationAreaArray(0).sizeOfAreaCircularRegionArray());
    }

    private Message<IncidentDocumentMessage> createNewRttaRequest(String fileName) {

        XmlObject rtta = DeldotTestUtils.getRttaElementFromFile(fileName, 0);

        IncidentDocumentMessage doc = new IncidentDocumentMessage();
        doc.setRtta(rtta);

        Message<IncidentDocumentMessage> message = new GenericMessage<IncidentDocumentMessage>(doc);

        return message;
    }

    private Message<IncidentDocumentMessage> createArchiveIncidentRequest() {

        IncidentDocumentMessage doc = new IncidentDocumentMessage();
        WorkProductDocument wpd = WorkProductDocument.Factory.newInstance();
        wpd.addNewWorkProduct().set(CommonTestUtils.getDefaultIncidentWorkProduct());
        doc.setWorkProductDocument(wpd);

        Message<IncidentDocumentMessage> message = new GenericMessage<IncidentDocumentMessage>(doc);

        return message;
    }

    private Message<IncidentDocumentMessage> createUpdateRttaRequest(String fileName) {

        XmlObject rtta = DeldotTestUtils.getRttaElementFromFile(fileName, 0);

        WorkProductDocument wpd = WorkProductDocument.Factory.newInstance();
        wpd.addNewWorkProduct();
        IdentificationType id = Common.createWorkProductIdentification("checksum", INCIDENT_ID1,
            "Incident", "1", Common.WORKPRODUCT_ACTIVE);

        PropertiesType props = Common.createWorkProductProperties("id",
            CommonTestUtils.VALID_ACTIVITY_DATE, "createdby", "1",
            CommonTestUtils.VALID_ACTIVITY_DATE, "lastUpdatedBy", "mimeCodespace", "codeValue");

        PackageMetadataType packageMetadata = wpd.getWorkProduct().addNewPackageMetadata();
        Common.setIdentifierElement(packageMetadata.addNewPackageMetadataExtensionAbstract(), id);
        Common.setPropertiesElement(packageMetadata.addNewPackageMetadataExtensionAbstract(), props);

        IncidentDocumentMessage doc = new IncidentDocumentMessage();
        doc.setRtta(rtta);
        doc.setWorkProductDocument(wpd);

        Message<IncidentDocumentMessage> message = new GenericMessage<IncidentDocumentMessage>(doc);

        return message;

    }
}
