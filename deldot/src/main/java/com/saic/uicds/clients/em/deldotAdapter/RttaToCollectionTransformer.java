package com.saic.uicds.clients.em.deldotAdapter;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.springframework.integration.Message;

public class RttaToCollectionTransformer {

    public List<IncidentDocumentMessage> handleRTTAXmlObject(Message<XmlObject> message) {

        XmlObject[] objects = message.getPayload().selectPath("//data/rtta");

        ArrayList<IncidentDocumentMessage> list = new ArrayList<IncidentDocumentMessage>();

        for (XmlObject rtta : objects) {
            IncidentDocumentMessage doc = new IncidentDocumentMessage();
            doc.setRtta(rtta);
            list.add(doc);
        }

        return list;

    }
}
