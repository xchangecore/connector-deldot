package com.saic.uicds.clients.em.deldotAdapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.xmlbeans.XmlObject;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.web.client.RestTemplate;

public class HTTPTimerTaskTest {

    private static final String URL = "http://www.nhc.noaa.gov/gtwo.xml";

    HTTPTimerTask httpTimerTask;

    RestTemplate restTemplate;
    MessageChannel channel;

    @Before
    public void setUp() throws Exception {

        httpTimerTask = new HTTPTimerTask();

        restTemplate = new RestTemplate();
        httpTimerTask.setRestTemplate(restTemplate);

        channel = EasyMock.createMock(MessageChannel.class);
        httpTimerTask.setOutputChannel(channel);

        httpTimerTask.setUrl(URL);

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testHttpTimerTask() {

        assertNotNull("null task", httpTimerTask);
        assertNotNull("no url", httpTimerTask.getUrl());
        assertEquals("wrong url", URL, httpTimerTask.getUrl());

    }

    @Test
    public void testRun() {

        Capture<Message> capturedMessage = new Capture<Message>();

        EasyMock.expect(
            channel.send(EasyMock.and(EasyMock.capture(capturedMessage),
                EasyMock.isA(Message.class)))).andReturn(true);

        EasyMock.replay(channel);
        httpTimerTask.run();

        EasyMock.verify(channel);

        Message<XmlObject> message = capturedMessage.getValue();
        assertNotNull("null message", message);

        XmlObject content = message.getPayload();
        assertNotNull("null content", content);
        XmlObject[] rsss = content.selectChildren("", "rss");
        assertEquals("no rss", 1, rsss.length);
    }

    @Test
    public void testRunWithParseError() {

        httpTimerTask.setUrl("http://www.google.com");

        EasyMock.replay(channel);

        httpTimerTask.run();

        EasyMock.verify(channel);
    }
}
