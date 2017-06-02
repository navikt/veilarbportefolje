package no.nav.fo.consumer;

import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class TilordningFeedHandler implements FeedCallback<BrukerOppdatertInformasjon> {

    private static final Logger LOG = getLogger(TilordningFeedHandler.class);

    private OppdaterBrukerdataFletter oppdaterBrukerdataFletter;

    @Inject
    public TilordningFeedHandler(OppdaterBrukerdataFletter oppdaterBrukerdataFletter) {
        this.oppdaterBrukerdataFletter = oppdaterBrukerdataFletter;
    }

    @Inject
    private JdbcTemplate db;

    @Override
    @Transactional
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {
        LOG.debug(String.format("Feed-data mottatt: %s", data));
        data.forEach(b -> oppdaterBrukerdataFletter.tilordneVeilederTilPersonId(b));
        Event event = MetricsFactory.createEvent("datamotattfrafeed");
        event.report();
        db.update("UPDATE METADATA SET tilordning_sist_oppdatert = ?", Date.from(ZonedDateTime.parse(lastEntryId).toInstant()));

    }
}
