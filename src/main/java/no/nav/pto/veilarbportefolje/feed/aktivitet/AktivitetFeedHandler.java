package no.nav.pto.veilarbportefolje.feed.aktivitet;

import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.sbl.featuretoggle.unleash.UnleashService;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;

import static no.nav.metrics.MetricsFactory.getMeterRegistry;
import static no.nav.pto.veilarbportefolje.util.DateUtils.*;

@Slf4j
public class AktivitetFeedHandler implements FeedCallback<AktivitetDataFraFeed> {

    private static final String FEED_NAME = "aktivitet";

    private BrukerRepository brukerRepository;
    private AktivitetService aktivitetService;
    private UnleashService unleashService;

    private static long lastEntryIdAsMillisSinceEpoch;

    @Inject
    public AktivitetFeedHandler(BrukerRepository brukerRepository,
                                AktivitetService aktivitetService,
                                UnleashService unleashService
                                ) {
        this.brukerRepository = brukerRepository;
        this.aktivitetService = aktivitetService;
        this.unleashService = unleashService;
        Gauge.builder("portefolje_feed_last_id", AktivitetFeedHandler::getLastEntryIdAsMillisSinceEpoch).tag("feed_name", FEED_NAME).register(getMeterRegistry());
    }

    private static long getLastEntryIdAsMillisSinceEpoch() {
        return lastEntryIdAsMillisSinceEpoch;
    }

    @Override
    public void call(String lastEntry, List<AktivitetDataFraFeed> data) {
        if(!unleashService.isEnabled("portefolje.kafka.aktiviteter")) {
            log.info("AktivitetfeedDebug data: {}", data);


            aktivitetService.oppdaterAktiviteter(data);

            Timestamp lastEntryId = timestampFromISO8601(lastEntry);
            lastEntryIdAsMillisSinceEpoch = lastEntryId.getTime();
            brukerRepository.setAktiviteterSistOppdatert(lastEntryId);
        } else {
            log.info("Konsumerer aktiviter på aapen-fo-endringPaaAktivitet-v1");
        }
    }
}
