package com.saic.uicds.clients.em.deldotAdapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import gov.niem.niem.niemCore.x20.ActivityDateDocument;
import gov.niem.niem.niemCore.x20.DateTimeDocument;
import gov.niem.niem.niemCore.x20.DateType;
import gov.ucore.ucore.x20.DigestType;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.uicds.incident.IncidentDocument;
import org.uicds.incidentManagementService.GetIncidentListRequestDocument;
import org.uicds.incidentManagementService.GetIncidentListResponseDocument;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.PropertiesType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.em.async.UicdsCore;
import com.saic.uicds.clients.util.Common;
import com.saic.uicds.clients.util.CommonTestUtils;

public class IncidentContentEnricherTest {

    private static final String RTTA_ID1 = "32883";
    private static final String RTTA_ID2 = "33519";

    private static final String REMOVED_RTTI_ID = "138";

    IncidentContentEnricher enricher;

    UicdsCore uicdsCore;

    @Before
    public void setUp() throws Exception {

        enricher = new IncidentContentEnricher();

        uicdsCore = EasyMock.createMock(UicdsCore.class);
        enricher.setUicdsCore(uicdsCore);
    }

    // no current incidents so create incidents for all rtta
    @Test
    public void testEnrichWithNoCurrentIncidents() {

        String fileName = "src/test/resources/rtta-1.xml";
        Message<List<IncidentDocumentMessage>> messages = getMessageListFromFile(fileName);

        GetIncidentListResponseDocument response = CommonTestUtils.createEmptyIncidentListResponse();

        EasyMock.expect(
            uicdsCore.marshalSendAndReceive(EasyMock.isA(GetIncidentListRequestDocument.class))).andReturn(
            response);

        EasyMock.replay(uicdsCore);

        Message<List<IncidentDocumentMessage>> enrichedMessage = enricher.enrichWithIncidentData(messages);

        assertNotNull("null response", enrichedMessage);
        assertNotNull("null payload", enrichedMessage.getPayload());
        assertFalse("no WP header", enrichedMessage.getHeaders().containsKey("WorkProduct"));
        for (IncidentDocumentMessage doc : enrichedMessage.getPayload()) {
            assertNotNull("rtta null", doc.getRtta());
            assertNull("wpd not null", doc.getWorkProductDocument());
        }

        // should have an item for each of the original rtta items
        int numRTTA = messages.getPayload().size();
        assertEquals("wrong # docs", numRTTA, enrichedMessage.getPayload().size());
    }

    // have current incidents but none match so create incidents for all
    @Test
    public void testEnrichWithIncidentListNoMatchingIncident() {

        String fileName = "src/test/resources/rtta-1.xml";
        Message<List<IncidentDocumentMessage>> messages = getMessageListFromFile(fileName);

        GetIncidentListResponseDocument response = CommonTestUtils.createIncidentListResponse();
        WorkProduct wp = CommonTestUtils.getEmptyIncidentWorkProduct();
        response.getGetIncidentListResponse().getWorkProductList().setWorkProductArray(0, wp);

        EasyMock.expect(
            uicdsCore.marshalSendAndReceive(EasyMock.isA(GetIncidentListRequestDocument.class))).andReturn(
            response);

        EasyMock.expect(uicdsCore.getWorkProductFromCore(EasyMock.isA(IdentificationType.class))).andReturn(
            wp);

        EasyMock.replay(uicdsCore);

        Message<List<IncidentDocumentMessage>> enrichedMessage = enricher.enrichWithIncidentData(messages);

        assertNotNull("null response", enrichedMessage);
        assertNotNull("null payload", enrichedMessage.getPayload());
        assertFalse("no WP header", enrichedMessage.getHeaders().containsKey("WorkProduct"));
        for (IncidentDocumentMessage doc : enrichedMessage.getPayload()) {
            assertNotNull("rtta null", doc.getRtta());
            assertNull("wpd not null", doc.getWorkProductDocument());
        }

        // should have an item for each of the original rtta items
        int numRTTA = messages.getPayload().size();
        assertEquals("wrong # docs", numRTTA, enrichedMessage.getPayload().size());
    }

    // have current incidents with one matching an RTTA so it should find a work product for it
    @Test
    public void testEnrichWithIncidentListMatchingIncident() {

        String fileName = "src/test/resources/rtta-1.xml";
        Message<List<IncidentDocumentMessage>> messages = getMessageListFromFile(fileName);

        GetIncidentListResponseDocument response = CommonTestUtils.createIncidentListResponse();
        WorkProduct wp = CommonTestUtils.getEmptyIncidentWorkProduct();
        response.getGetIncidentListResponse().getWorkProductList().addNewWorkProduct().set(wp);

        WorkProduct matchingWP = addRttaIncidentToIncidentListResponse(response, RTTA_ID1, null);

        EasyMock.expect(
            uicdsCore.marshalSendAndReceive(EasyMock.isA(GetIncidentListRequestDocument.class))).andReturn(
            response);

        EasyMock.expect(uicdsCore.getWorkProductFromCore(EasyMock.isA(IdentificationType.class))).andReturn(
            matchingWP).anyTimes();

        EasyMock.replay(uicdsCore);

        Message<List<IncidentDocumentMessage>> enrichedMessage = enricher.enrichWithIncidentData(messages);

        assertNotNull("null response", enrichedMessage);
        assertNotNull("null payload", enrichedMessage.getPayload());

        // Check that the item we matched has a work product
        checkEnrichedMessagesWithOneMatching(enrichedMessage, RTTA_ID1);

        // should have an item for each of the original rtta items
        int numRTTA = messages.getPayload().size();
        assertEquals("wrong # docs", numRTTA, enrichedMessage.getPayload().size());
    }

    // have current incidents with one RTTA but the incoming list of rttas no longer contains that
    // item
    @Test
    public void testEnrichWithRemovedRTTAItem() {

        String fileName = "src/test/resources/rtta-1.xml";
        Message<List<IncidentDocumentMessage>> messages = getMessageListFromFile(fileName);

        GetIncidentListResponseDocument response = CommonTestUtils.createIncidentListResponse();
        // WorkProduct wp = CommonTestUtils.getEmptyIncidentWorkProduct();
        // response.getGetIncidentListResponse().getWorkProductList().addNewWorkProduct().set(wp);

        WorkProduct matchingWP = addRttaIncidentToIncidentListResponse(response, REMOVED_RTTI_ID,
            null);

        EasyMock.expect(
            uicdsCore.marshalSendAndReceive(EasyMock.isA(GetIncidentListRequestDocument.class))).andReturn(
            response);

        EasyMock.expect(uicdsCore.getWorkProductFromCore(EasyMock.isA(IdentificationType.class))).andReturn(
            matchingWP).anyTimes();

        EasyMock.replay(uicdsCore);

        Message<List<IncidentDocumentMessage>> enrichedMessage = enricher.enrichWithIncidentData(messages);

        assertNotNull("null response", enrichedMessage);
        assertNotNull("null payload", enrichedMessage.getPayload());

        // Check that the item we matched has a work product
        checkEnrichedMessagesWithOneToRemove(enrichedMessage, REMOVED_RTTI_ID);

        // should have an item for each of the original rtta items + one to remove
        int numRTTA = messages.getPayload().size() + 1;
        assertEquals("wrong # docs", numRTTA, enrichedMessage.getPayload().size());
    }

    @Test
    public void testUpdate() {

        String fileName = "src/test/resources/rtta-4.xml";
        Message<List<IncidentDocumentMessage>> messages = getMessageListFromFile(fileName);

        GetIncidentListResponseDocument response = CommonTestUtils.createEmptyIncidentListResponse();

        WorkProduct incident1 = addRttaIncidentToIncidentListResponse(response, RTTA_ID1,
            "2011-02-22T17:55:00");
        WorkProduct incident2 = addRttaIncidentToIncidentListResponse(response, RTTA_ID2, null);

        EasyMock.expect(
            uicdsCore.marshalSendAndReceive(EasyMock.isA(GetIncidentListRequestDocument.class))).andReturn(
            response);

        EasyMock.expect(uicdsCore.getWorkProductFromCore(EasyMock.isA(IdentificationType.class))).andReturn(
            incident1).anyTimes();

        EasyMock.expect(uicdsCore.getWorkProductFromCore(EasyMock.isA(IdentificationType.class))).andReturn(
            incident2).anyTimes();

        EasyMock.replay(uicdsCore);

        Message<List<IncidentDocumentMessage>> enrichedMessage = enricher.enrichWithIncidentData(messages);

        assertEquals("wrong #", 1, enrichedMessage.getPayload().size());

    }

    private void checkEnrichedMessagesWithOneMatching(
        Message<List<IncidentDocumentMessage>> enrichedMessage, String rttaID) {

        boolean foundMatching = false;
        for (IncidentDocumentMessage doc : enrichedMessage.getPayload()) {
            assertNotNull("no item", doc);
            assertNotNull("no rtta", doc.getRtta());
            XmlObject ids[] = doc.getRtta().selectChildren("", "id");
            if (ids.length > 0) {
                String id = Common.getTextFromAny(ids[0]);
                if (id.equals(rttaID)) {
                    assertNotNull("no wpd", doc.getWorkProductDocument());
                    foundMatching = true;
                }
            }
        }
        assertTrue("matching not found", foundMatching);
    }

    private void checkEnrichedMessagesWithOneToRemove(
        Message<List<IncidentDocumentMessage>> enrichedMessage, String rttaID) {

        boolean foundMatching = false;
        for (IncidentDocumentMessage doc : enrichedMessage.getPayload()) {
            assertNotNull("no item", doc);
            // null rtta will indicate there is a related incident to remove
            if (doc.getRtta() == null) {
                assertNotNull("no wpd", doc.getWorkProductDocument());
                IncidentDocument incident = Common.getIncidentDocumentFromWorkProduct(doc.getWorkProductDocument().getWorkProduct());
                String deldotID = IncidentContentEnricher.findDeldotID(incident.getIncident());
                if (deldotID.equals(rttaID)) {
                    foundMatching = true;
                }
            }
        }
        assertTrue("matching not found", foundMatching);
    }

    private WorkProduct addRttaIncidentToIncidentListResponse(
        GetIncidentListResponseDocument response, String rttaID, String date) {

        DigestType digest = DigestType.Factory.newInstance();
        CommonTestUtils.setEvent(digest, CommonTestUtils.EVENT_ID1,
            CommonTestUtils.DEFAULT_ACTIVITY_DESCRIPTION, "incident", null, null);
        IdentificationType wpid = Common.createWorkProductIdentification("check", "Incident-"
            + rttaID, "Incident", "1", "Active");
        PropertiesType props = Common.createWorkProductProperties(CommonTestUtils.IG_ID1 + rttaID,
            CommonTestUtils.VALID_PROPERTIES_DATE, "createdBy", "1",
            CommonTestUtils.VALID_PROPERTIES_DATE, "lastUpdatedBy", "mime", "mime");
        WorkProduct matchingWP = CommonTestUtils.getIncidentWorkProduct(wpid, props, digest, null);
        CommonTestUtils.addIncidentEventToIncidentAndUpdateModifedByAndTime(matchingWP, "CREATED",
            "DELDOT", rttaID, "RTTA", "deldot");

        IncidentDocument incident = Common.getIncidentDocumentFromWorkProduct(matchingWP);

        if (date != null) {
            // 2011-02-22 17:40:38.0
            DateTimeDocument dateDoc = DateTimeDocument.Factory.newInstance();
            dateDoc.addNewDateTime().setStringValue(date);

            ActivityDateDocument activityDate = ActivityDateDocument.Factory.newInstance();
            activityDate.addNewActivityDate().set(dateDoc);

            Common.substitute(incident.getIncident().addNewActivityDateRepresentation(),
                Common.NIEM_NS, Common.ACTIVITY_DATE, DateType.type, activityDate.getActivityDate());

            matchingWP.getStructuredPayloadArray(0).set(incident);
        }

        response.getGetIncidentListResponse().getWorkProductList().addNewWorkProduct().set(
            matchingWP);
        return matchingWP;
    }

    private Message<List<IncidentDocumentMessage>> getMessageListFromFile(String fileName) {

        XmlObject data = DeldotTestUtils.getRttaDocFromFile(fileName);
        XmlObject[] rttas = data.selectPath("data/rtta");

        Message<List<IncidentDocumentMessage>> messages = null;
        ArrayList<IncidentDocumentMessage> list = new ArrayList<IncidentDocumentMessage>();
        for (XmlObject rtta : rttas) {
            IncidentDocumentMessage doc = new IncidentDocumentMessage();
            doc.setRtta(rtta);
            list.add(doc);
            messages = new GenericMessage<List<IncidentDocumentMessage>>(list);
        }

        return messages;
    }

}
