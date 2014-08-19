package com.saic.uicds.clients.em.deldotAdapter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.message.GenericMessage;

public class DeldotAdapter {
    private static final String APP_CONTEXT_FILE = "deldotAdapter-context.xml";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * @param args
     */
    public static void main(String[] args) {

        // default the protocol if not specified
        if (args.length == 1) {
            usage();
            return;
        }

        ApplicationContext context = getApplicationContext();

        // sendTestMessage(context);

        // sendTestMessages(context);

        // HTTPTimerTask httpTimerTask = (HTTPTimerTask) context.getBean("httpTimerTask");
        //
        // if (httpTimerTask != null) {
        // httpTimerTask.run();
        // }

    }

    private static void sendTestMessage(ApplicationContext context) {

        MessageChannel channel = (MessageChannel) context.getBean("inboundRTTADataChannel");

        XmlObject rtta = getRttaDocFromFile("src/test/resources/rtta-1.xml");
        sendMessage(channel, rtta);

    }

    private static void sendTestMessages(ApplicationContext context) {

        MessageChannel channel = (MessageChannel) context.getBean("inboundRTTADataChannel");

        XmlObject rtta = getRttaDocFromFile("src/test/resources/rtta-3.xml");
        sendMessage(channel, rtta);

        int millies = 20000;
        sleep(millies);

        rtta = getRttaDocFromFile("src/test/resources/rtta-4.xml");
        sendMessage(channel, rtta);
    }

    public static void sendMessage(MessageChannel channel, XmlObject rtta) {

        Message<XmlObject> message = new GenericMessage<XmlObject>(rtta);
        channel.send(message);
    }

    public static void sleep(int millies) {

        try {
            Thread.sleep(millies);
        } catch (InterruptedException e) {
            System.err.println("Sleep interrupted");
        }
    }

    private static ApplicationContext getApplicationContext() {

        ApplicationContext context = null;
        try {
            context = new FileSystemXmlApplicationContext("./" + APP_CONTEXT_FILE);
            System.out.println("Using local application context file: " + APP_CONTEXT_FILE);
        } catch (BeansException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                System.out.println("Local application context file not found so using file from jar: contexts/"
                    + APP_CONTEXT_FILE);
            } else {
                // System.out.println("Error reading local file context: " +
                // e.getCause().getMessage());
                e.printStackTrace();
            }
        }

        if (context == null) {
            context = new ClassPathXmlApplicationContext(new String[] { "contexts/"
                + APP_CONTEXT_FILE });
        }

        return context;
    }

    private static void usage() {

        System.out.println("");
        System.out.println("This is the UICDS DELDOT Adapter.");
        System.out.println("Execution of this client depends on a functioning UICDS server. The default is http://localhost/uicds/core/ws/services");
        System.out.println("To verify that a UICDS server is accessible, use a browser to navigate to http://localhost/uicds/Console.html");
        System.out.println("");
        System.out.println("Usage: java -jar deldotAdapter.jar");
        System.out.println("");
        System.out.println("Parameters for the tpmAdapter can be configued in Spring context file");
        System.out.println("in the current directory or classpath named: " + APP_CONTEXT_FILE);
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
