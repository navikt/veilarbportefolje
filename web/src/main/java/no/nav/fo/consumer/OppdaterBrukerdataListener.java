package no.nav.fo.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import org.slf4j.Logger;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.TextMessage;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class OppdaterBrukerdataListener {

    private static final Logger LOG = getLogger(OppdaterBrukerdataListener.class);

    @Inject
    private OppdaterBrukerdataFletter oppdaterBrukerdataFletter;

    @JmsListener(id = "endringAvVeileder_inbound", containerFactory = "jmsListenerContainerFactory", destination = "java:jboss/jms/endreVeilederKo")
    public void listenForEndringAvVeileder(Object message) {
        TextMessage textMessage = (TextMessage) message;

        try {
            String melding = textMessage.getText();
            LOG.debug(String.format("Melding motatt: %s", melding));
            oppdaterBrukerdataFletter.tilordneVeilederTilPersonId(konverterJSONTilBruker(melding));
        } catch (JMSException e) {
            LOG.error("Kunne ikke lese melding fra kø", e);
        }
    }

    public BrukerOppdatertInformasjon konverterJSONTilBruker(String brukerString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            BrukerOppdatertInformasjon bruker = mapper.readValue(brukerString,BrukerOppdatertInformasjon.class);
            return bruker;
        } catch (IOException e) {
            LOG.error("Kunne ikke lese brukerinformasjon",e);
        }
        return null;
    }



}
