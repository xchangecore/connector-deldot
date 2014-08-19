package com.saic.uicds.clients.em.deldotAdapter;

import org.apache.xmlbeans.XmlObject;

import com.saic.precis.x2009.x06.structures.WorkProductDocument;

public class IncidentDocumentMessage {

    private XmlObject rtta;

    private WorkProductDocument workProductDocument;

    /**
     * @return the rtta
     */
    public XmlObject getRtta() {

        return rtta;
    }

    /**
     * @param rtta the rtta to set
     */
    public void setRtta(XmlObject rtta) {

        this.rtta = rtta;
    }

    /**
     * @return the workProductDocument
     */
    public WorkProductDocument getWorkProductDocument() {

        return workProductDocument;
    }

    /**
     * @param workProductDocument the workProductDocument to set
     */
    public void setWorkProductDocument(WorkProductDocument workProductDocument) {

        this.workProductDocument = workProductDocument;
    }

}
