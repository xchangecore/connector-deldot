package com.saic.uicds.clients.em.deldotAdapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

public class DeldotTestUtils {

    public static XmlObject getRttaElementFromFile(String fileName, int index) {

        XmlObject doc = getRttaDocFromFile(fileName);

        assertNotNull("null data", doc);
        XmlObject[] datas = doc.selectChildren("", "data");
        assertEquals("no datas", 1, datas.length);
        XmlObject[] rttas = datas[0].selectChildren("", "rtta");
        int size = rttas.length;
        assertTrue("no rtta", size > 0);

        if (index > rttas.length - 1) {
            fail("invalid index " + index + " length is only " + rttas.length);
        }
        return rttas[index];
    }

    public static XmlObject getRttaDocFromFile(String fileName) {

        File file = new File(fileName);
        assertNotNull("null file", file);
        assertTrue("file doesn't exist", file.exists());
        try {
            XmlObject doc = XmlObject.Factory.parse(file);
            return doc;
        } catch (XmlException e) {
            fail("XML parse exception: " + e.getMessage());
        } catch (IOException e) {
            fail("IO exception " + e.getMessage());
        }

        return null;
    }

}
