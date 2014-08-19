package com.saic.uicds.clients.em.deldotAdapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;

public class RttaToCollectionTransformerTest {

    RttaToCollectionTransformer transformer;

    @Before
    public void setUp() throws Exception {

        transformer = new RttaToCollectionTransformer();
    }

    @Test
    public void testRTTAToCollection() {

        assertNotNull(transformer);

        String fileName = "src/test/resources/rtta-1.xml";
        XmlObject doc = DeldotTestUtils.getRttaDocFromFile(fileName);

        assertNotNull("null data", doc);
        XmlObject[] datas = doc.selectChildren("", "data");
        assertEquals("no datas", 1, datas.length);
        XmlObject[] rttas = datas[0].selectChildren("", "rtta");
        int size = rttas.length;
        assertTrue("no rtta", size > 0);

        Message<XmlObject> message = new GenericMessage<XmlObject>(doc);
        List<IncidentDocumentMessage> rttaItems = transformer.handleRTTAXmlObject(message);

        assertEquals("wrong # items", size, rttaItems.size());

    }

}
