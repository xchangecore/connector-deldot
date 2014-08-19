package com.saic.uicds.clients.em.deldotAdapter;

import java.util.TimerTask;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class HTTPTimerTask
    extends TimerTask {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private RestTemplate restTemplate;

    private MessageChannel outputChannel;

    private String url;

    /**
     * @return the restTemplate
     */
    public RestTemplate getRestTemplate() {

        return restTemplate;
    }

    /**
     * @param restTemplate the restTemplate to set
     */
    public void setRestTemplate(RestTemplate restTemplate) {

        this.restTemplate = restTemplate;
    }

    /**
     * @return the outputChannel
     */
    public MessageChannel getOutputChannel() {

        return outputChannel;
    }

    /**
     * @param outputChannel the outputChannel to set
     */
    public void setOutputChannel(MessageChannel outputChannel) {

        this.outputChannel = outputChannel;
    }

    /**
     * @return the url
     */
    public String getUrl() {

        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {

        this.url = url;
    }

    @Override
    public void run() {

        logger.info("Polling " + url);

        XmlObject result = getData();

        if (result != null) {
            // System.out.println(result.xmlText());

            Message<XmlObject> message = new GenericMessage<XmlObject>(result);

            outputChannel.send(message);
        }

    }

    private XmlObject getData() {

        String resultStr = null;
        try {
            resultStr = restTemplate.getForObject(url, String.class);
        } catch (HttpStatusCodeException e) {
            logger.error("HTTP error polling DELDOT: " + e.getMessage());
            logger.error("HTTP Status Code: " + e.getStatusCode());
            logger.error("HTTP Status Text: " + e.getStatusText());
            logger.error("HTTP Response   : " + e.getResponseBodyAsString());

        } catch (RestClientException e) {
            logger.error("Error polling DELDOT: " + e.getMessage());
        }

        if (resultStr != null) {
            try {
                XmlObject result = XmlObject.Factory.parse(resultStr);
                return result;
            } catch (XmlException e) {
                logger.error("Error parsing result: " + e.getMessage());
                logger.error(resultStr);
                return null;
            }
        }
        return null;
    }
}
