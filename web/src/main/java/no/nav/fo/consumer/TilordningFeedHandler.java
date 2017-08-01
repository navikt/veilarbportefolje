package no.nav.fo.consumer;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.fo.util.OppfolgingUtils;
import no.nav.fo.util.MetricsUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class TilordningFeedHandler implements FeedCallback<BrukerOppdatertInformasjon> {

    private static final Logger LOG = getLogger(TilordningFeedHandler.class);

    private OppdaterBrukerdataFletter oppdaterBrukerdataFletter;
    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;

    @Inject
    public TilordningFeedHandler(OppdaterBrukerdataFletter oppdaterBrukerdataFletter, ArbeidslisteService arbeidslisteService, BrukerRepository brukerRepository) {
        this.oppdaterBrukerdataFletter = oppdaterBrukerdataFletter;
        this.arbeidslisteService = arbeidslisteService;
        this.brukerRepository = brukerRepository;
    }

    @Inject
    private JdbcTemplate db;

    @Override
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {
        LOG.debug(String.format("Feed-data mottatt: %s", data));
        data.forEach(this::behandleObjektFraFeed);
        db.update("UPDATE METADATA SET tilordning_sist_oppdatert = ?", Date.from(ZonedDateTime.parse(lastEntryId).toInstant()));
        Event event = MetricsFactory.createEvent("datamotattfrafeed");
        event.report();
    }

    private void behandleObjektFraFeed(BrukerOppdatertInformasjon bruker) {
        try {
            MetricsUtils.timed(
                    "feed.situasjon.objekt",
                    () -> {
                        if(OppfolgingUtils.skalArbeidslisteSlettes(bruker, brukerRepository)) {
                            arbeidslisteService.deleteArbeidsliste(new AktoerId(bruker.getAktoerid()));
                        }
                        oppdaterBrukerdataFletter.tilordneVeilederTilPersonId(bruker);
                        return null;
                        },
                    (timer, hasFailed) -> { if(hasFailed) {timer.addTagToReport("aktorhash", DigestUtils.md5Hex(bruker.getAktoerid()).toUpperCase());}}
            );
        }catch(Exception e) {
            LOG.error("Feil ved behandling av objekt fra feed med aktorid {}, {}", bruker.getAktoerid(), e.getMessage());
        }
    }
}
