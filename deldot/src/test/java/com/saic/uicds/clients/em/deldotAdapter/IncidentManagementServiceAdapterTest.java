package com.saic.uicds.clients.em.deldotAdapter;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.uicds.incident.IncidentDocument;
import org.uicds.incidentManagementService.ArchiveIncidentRequestDocument;
import org.uicds.incidentManagementService.ArchiveIncidentResponseDocument;
import org.uicds.incidentManagementService.CloseIncidentRequestDocument;
import org.uicds.incidentManagementService.CloseIncidentResponseDocument;
import org.uicds.incidentManagementService.CreateIncidentRequestDocument;
import org.uicds.incidentManagementService.CreateIncidentResponseDocument;
import org.uicds.incidentManagementService.UpdateIncidentRequestDocument;
import org.uicds.incidentManagementService.UpdateIncidentResponseDocument;

import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.precis.x2009.x06.structures.WorkProductDocument.WorkProduct;
import com.saic.uicds.clients.em.async.UicdsCore;
import com.saic.uicds.clients.util.CommonTestUtils;

public class IncidentManagementServiceAdapterTest {

    private UicdsCore uicdsCore;

    private IncidentManagementServiceAdapter adapter;

    @Before
    public void setUp() throws Exception {

        adapter = new IncidentManagementServiceAdapter();
        uicdsCore = EasyMock.createMock(UicdsCore.class);
        adapter.setUicdsCore(uicdsCore);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCreate() {

        CreateIncidentResponseDocument response = CommonTestUtils.createCreateIncidentResponse();

        EasyMock.expect(
            uicdsCore.marshalSendAndReceive(EasyMock.isA(CreateIncidentRequestDocument.class))).andReturn(
            response);

        EasyMock.replay(uicdsCore);

        Message<WorkProductDocument> message = getCreateIncidentWorkProduct();

        adapter.processIncidentWorkProduct(message);

        EasyMock.verify(uicdsCore);
    }

    @Test
    public void testUpdate() {

        UpdateIncidentResponseDocument response = CommonTestUtils.createUpdateIncidentResponse();

        EasyMock.expect(
            uicdsCore.marshalSendAndReceive(EasyMock.isA(UpdateIncidentRequestDocument.class))).andReturn(
            response);

        EasyMock.replay(uicdsCore);

        Message<WorkProductDocument> message = getUpdateIncidentWorkProduct();

        adapter.processIncidentWorkProduct(message);

        EasyMock.verify(uicdsCore);
    }

    @Test
    public void testArchive() {

        CloseIncidentResponseDocument closeResponse = CommonTestUtils.createCloseIncidentResponse();

        EasyMock.expect(
            uicdsCore.marshalSendAndReceive(EasyMock.isA(CloseIncidentRequestDocument.class))).andReturn(
            closeResponse);

        ArchiveIncidentResponseDocument archiveResponse = CommonTestUtils.createArchiveIncidentResponse();

        EasyMock.expect(
            uicdsCore.marshalSendAndReceive(EasyMock.isA(ArchiveIncidentRequestDocument.class))).andReturn(
            archiveResponse);

        EasyMock.replay(uicdsCore);

        Message<WorkProductDocument> message = getArchiveIncidentWorkProduct();

        adapter.processIncidentWorkProduct(message);

        EasyMock.verify(uicdsCore);
    }

    private Message<WorkProductDocument> getArchiveIncidentWorkProduct() {

        WorkProductDocument wpd = WorkProductDocument.Factory.newInstance();

        WorkProduct wp = CommonTestUtils.getDefaultIncidentWorkProduct();

        wpd.addNewWorkProduct().set(wp);

        return MessageBuilder.withPayload(wpd).setHeaderIfAbsent("ARCHIVE", true).build();
    }

    private Message<WorkProductDocument> getCreateIncidentWorkProduct() {

        WorkProductDocument wpd = WorkProductDocument.Factory.newInstance();

        IncidentDocument incident = CommonTestUtils.getDefaultIncident();

        wpd.addNewWorkProduct().addNewStructuredPayload().set(incident);

        return new GenericMessage<WorkProductDocument>(wpd);
    }

    private Message<WorkProductDocument> getUpdateIncidentWorkProduct() {

        WorkProductDocument wpd = WorkProductDocument.Factory.newInstance();

        WorkProduct wp = CommonTestUtils.getDefaultIncidentWorkProduct();

        wpd.addNewWorkProduct().set(wp);

        return new GenericMessage<WorkProductDocument>(wpd);

    }
}
