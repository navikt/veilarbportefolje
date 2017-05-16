package no.nav.fo.consumer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
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
    static ObjectMapper mapper = new ObjectMapper();

    static {
        SimpleModule module = new SimpleModule();

        module.addDeserializer(String.class, new JsonDeserializer<String>() {
            @Override
            public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
                String result = StringDeserializer.instance.deserialize(jsonParser, deserializationContext);

                if ("null".equals(result)) {
                    return null;
                }
                return result;
            }
        });

        mapper.registerModule(module);
    }

    private static final Logger LOG = getLogger(OppdaterBrukerdataListener.class);

    @Inject
    private OppdaterBrukerdataFletter oppdaterBrukerdataFletter;

    @Transactional
    @JmsListener(id = "endringAvVeileder_inbound", containerFactory = "jmsListenerContainerFactory", destination = "java:jboss/jms/endreVeilederKo")
    public void listenForEndringAvVeileder(Object message) {
        TextMessage textMessage = (TextMessage) message;

        try {
            String melding = textMessage.getText();
            LOG.debug(String.format("Melding motatt: %s", melding));
            oppdaterBrukerdataFletter.tilordneVeilederTilPersonId(konverterJSONTilBruker(melding));
            Event event = MetricsFactory.createEvent("meldinglestfrako");
            event.report();
        } catch (JMSException e) {
            LOG.error("Kunne ikke lese melding fra kø", e);
        }
    }

    public BrukerOppdatertInformasjon konverterJSONTilBruker(String brukerString) {
        try {
            BrukerOppdatertInformasjon bruker = mapper.readValue(brukerString, BrukerOppdatertInformasjon.class);
            return bruker;
        } catch (IOException e) {
            LOG.error("Kunne ikke lese brukerinformasjon", e);
        }
        return null;
    }
}
