package no.nav.fo.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class TilordningFeedHandler {

    private static final Logger LOG = getLogger(TilordningFeedHandler.class);

    private OppdaterBrukerdataFletter oppdaterBrukerdataFletter;

    @Inject
    public TilordningFeedHandler(OppdaterBrukerdataFletter oppdaterBrukerdataFletter) {
        this.oppdaterBrukerdataFletter = oppdaterBrukerdataFletter;
    }

    @Transactional
    public void handleFeedPage(List<BrukerOppdatertInformasjon> brukerOppdateringer) {

        LOG.debug(String.format("Feed-data mottatt: %s", brukerOppdateringer));
        brukerOppdateringer.forEach(b -> oppdaterBrukerdataFletter.tilordneVeilederTilPersonId(b));
        Event event = MetricsFactory.createEvent("datamotattfrafeed");
        event.report();
    }

    public BrukerOppdatertInformasjon konverterJSONTilBruker(String brukerString) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            BrukerOppdatertInformasjon bruker = mapper.readValue(brukerString, BrukerOppdatertInformasjon.class);
            return bruker;
        } catch (IOException e) {
            LOG.error("Kunne ikke lese brukerinformasjon", e);
        }
        return null;
    }


}
