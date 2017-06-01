package no.nav.fo.consumer;

import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class TilordningFeedHandler implements FeedCallback<BrukerOppdatertInformasjon> {

    private static final Logger LOG = getLogger(TilordningFeedHandler.class);

    private OppdaterBrukerdataFletter oppdaterBrukerdataFletter;

    @Inject
    public TilordningFeedHandler(OppdaterBrukerdataFletter oppdaterBrukerdataFletter) {
        this.oppdaterBrukerdataFletter = oppdaterBrukerdataFletter;
    }

    @Override
    @Transactional
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {
        LOG.debug(String.format("Feed-data mottatt: %s", data));
        data.forEach(b -> oppdaterBrukerdataFletter.tilordneVeilederTilPersonId(b));
        Event event = MetricsFactory.createEvent("datamotattfrafeed");
        event.report();
    }
}
