package no.nav.fo.consumer;

import org.slf4j.Logger;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.JMSException;
import javax.jms.TextMessage;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class EndringAvVeilederListener {

    private static final Logger LOG = getLogger(EndringAvVeilederListener.class);

    @Transactional
    @JmsListener(id = "endringAvVeileder_inbound", containerFactory = "jmsListenerContainerFactory", destination = "java:jboss/jms/endreVeilederKo")
    public void listenForEndringAvVeileder(Object message) {
        TextMessage textMessage = (TextMessage) message;
        String melding = "";
        try {
            melding = textMessage.getText();
        } catch (JMSException e) {
            e.printStackTrace();
        }
        LOG.debug(String.format("Melding motatt: %s", melding));
    }
}
