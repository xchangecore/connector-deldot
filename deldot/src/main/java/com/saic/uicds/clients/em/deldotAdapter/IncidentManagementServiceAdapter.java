package com.saic.uicds.clients.em.deldotAdapter;

import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.ServiceActivator;
import org.uicds.incident.UICDSIncidentType;
import org.uicds.incidentManagementService.ArchiveIncidentRequestDocument;
import org.uicds.incidentManagementService.ArchiveIncidentResponseDocument;
import org.uicds.incidentManagementService.CloseIncidentRequestDocument;
import org.uicds.incidentManagementService.CloseIncidentResponseDocument;
import org.uicds.incidentManagementService.CreateIncidentRequestDocument;
import org.uicds.incidentManagementService.CreateIncidentResponseDocument;
import org.uicds.incidentManagementService.UpdateIncidentRequestDocument;
import org.uicds.incidentManagementService.UpdateIncidentResponseDocument;

import com.saic.precis.x2009.x06.base.IdentificationType;
import com.saic.precis.x2009.x06.base.IdentifierType;
import com.saic.precis.x2009.x06.base.ProcessingStateType;
import com.saic.precis.x2009.x06.structures.WorkProductDocument;
import com.saic.uicds.clients.em.async.UicdsCore;
import com.saic.uicds.clients.util.Common;

public class IncidentManagementServiceAdapter {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private UicdsCore uicdsCore;

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

    @ServiceActivator
    public void processIncidentWorkProduct(Message<WorkProductDocument> message) {

        IdentificationType workProductID = Common.getIdentificationElement(message.getPayload().getWorkProduct());

        if (message.getHeaders().containsKey("ARCHIVE")) {
            closeAndArchiveIncident(message);
        } else {
            if (workProductID == null) {
                createIncident(message);
            } else {
                updateIncident(message);
            }
        }
    }

    private void closeAndArchiveIncident(Message<WorkProductDocument> message) {

        closeIncident(message);
        archiveIncident(message);

    }

    private void closeIncident(Message<WorkProductDocument> message) {

        CloseIncidentRequestDocument request = CloseIncidentRequestDocument.Factory.newInstance();

        IdentifierType igid = Common.getFirstAssociatedInterestGroup(message.getPayload().getWorkProduct());

        request.addNewCloseIncidentRequest().setIncidentID(igid.getStringValue());

        XmlObject response = uicdsCore.marshalSendAndReceive(request);

        CloseIncidentResponseDocument closeResponse = null;

        if (response instanceof CloseIncidentResponseDocument) {
            closeResponse = (CloseIncidentResponseDocument) response;

            if (closeResponse.getCloseIncidentResponse().getWorkProductProcessingStatus().getStatus() == ProcessingStateType.ACCEPTED) {

            } else {
                logger.error("Close Incident not accepted: "
                    + closeResponse.getCloseIncidentResponse().getWorkProductProcessingStatus().getStatus());
            }
        } else {
            logger.error("Error closing incident: " + response.xmlText());
        }
    }

    private void archiveIncident(Message<WorkProductDocument> message) {

        ArchiveIncidentRequestDocument request = ArchiveIncidentRequestDocument.Factory.newInstance();

        IdentifierType igid = Common.getFirstAssociatedInterestGroup(message.getPayload().getWorkProduct());

        request.addNewArchiveIncidentRequest().setIncidentID(igid.getStringValue());

        XmlObject response = uicdsCore.marshalSendAndReceive(request);

        ArchiveIncidentResponseDocument archiveResponse = null;

        if (response instanceof ArchiveIncidentResponseDocument) {
            archiveResponse = (ArchiveIncidentResponseDocument) response;

            if (archiveResponse.getArchiveIncidentResponse().getWorkProductProcessingStatus().getStatus() == ProcessingStateType.ACCEPTED) {

            } else {
                logger.error("Archive Incident not accepted: "
                    + archiveResponse.getArchiveIncidentResponse().getWorkProductProcessingStatus().getStatus());
            }
        } else {
            logger.error("Error archiving incident: " + response.xmlText());
        }
    }

    private void createIncident(Message<WorkProductDocument> message) {

        CreateIncidentRequestDocument request = CreateIncidentRequestDocument.Factory.newInstance();

        UICDSIncidentType incident = Common.getIncidentDocumentFromWorkProduct(
            message.getPayload().getWorkProduct()).getIncident();
        request.addNewCreateIncidentRequest().addNewIncident().set(incident);

        XmlObject response = uicdsCore.marshalSendAndReceive(request);

        CreateIncidentResponseDocument createResponse = null;

        if (response instanceof CreateIncidentResponseDocument) {
            createResponse = (CreateIncidentResponseDocument) response;

            if (createResponse.getCreateIncidentResponse().getWorkProductPublicationResponse().getWorkProductProcessingStatus().getStatus() == ProcessingStateType.ACCEPTED) {

            } else {
                logger.error("Create Incident not accepted: "
                    + createResponse.getCreateIncidentResponse().getWorkProductPublicationResponse().getWorkProductProcessingStatus().getStatus());
            }
        } else {
            logger.error("Error creating incident: " + response.xmlText());
        }

    }

    private void updateIncident(Message<WorkProductDocument> message) {

        UpdateIncidentRequestDocument request = UpdateIncidentRequestDocument.Factory.newInstance();

        UICDSIncidentType incident = Common.getIncidentDocumentFromWorkProduct(
            message.getPayload().getWorkProduct()).getIncident();
        request.addNewUpdateIncidentRequest().addNewIncident().set(incident);

        IdentificationType id = Common.getIdentificationElement(message.getPayload().getWorkProduct());
        request.getUpdateIncidentRequest().addNewWorkProductIdentification().set(id);

        XmlObject response = uicdsCore.marshalSendAndReceive(request);

        UpdateIncidentResponseDocument createResponse = null;

        if (response instanceof UpdateIncidentResponseDocument) {
            createResponse = (UpdateIncidentResponseDocument) response;

            if (createResponse.getUpdateIncidentResponse().getWorkProductPublicationResponse().getWorkProductProcessingStatus().getStatus() == ProcessingStateType.ACCEPTED) {

            } else {
                logger.error("Update Incident not accepted: "
                    + createResponse.getUpdateIncidentResponse().getWorkProductPublicationResponse().getWorkProductProcessingStatus().getStatus());
            }
        } else {
            logger.error("Error updating incident: " + response.xmlText());
        }
    }

}
