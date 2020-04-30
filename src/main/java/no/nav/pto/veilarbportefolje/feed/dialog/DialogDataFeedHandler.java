package no.nav.pto.veilarbportefolje.feed.dialog;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.result.Result;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static no.nav.metrics.MetricsFactory.getMeterRegistry;
import static no.nav.pto.veilarbportefolje.util.DateUtils.calculateTimeElapsed;
import static no.nav.pto.veilarbportefolje.util.DateUtils.timestampFromISO8601;


@Slf4j
public class DialogDataFeedHandler implements FeedCallback<DialogDataFraFeed> {

    private static final String FEED_NAME = "dialog";
    private final Counter antallTotaltMetrikk;
    private static long lastEntryIdAsMillisSinceEpoch;

    public static final String DIALOGAKTOR_SIST_OPPDATERT = "dialogaktor_sist_oppdatert";
    private final BrukerRepository brukerRepository;
    private final ElasticIndexer elasticIndexer;
    private final DialogFeedRepository dialogFeedRepository;
    private final UnleashService unleashService;

    @Inject
    public DialogDataFeedHandler(BrukerRepository brukerRepository,
                                 ElasticIndexer elasticIndexer,
                                 DialogFeedRepository dialogFeedRepository,
                                 UnleashService unleashService) {
        this.brukerRepository = brukerRepository;
        this.elasticIndexer = elasticIndexer;
        this.dialogFeedRepository = dialogFeedRepository;
        this.unleashService = unleashService;

        antallTotaltMetrikk = Counter.builder("portefolje_feed").tag("feed_name", FEED_NAME).register(getMeterRegistry());
        Gauge.builder("portefolje_feed_last_id", DialogDataFeedHandler::getLastEntryIdAsMillisSinceEpoch).tag("feed_name", FEED_NAME).register(getMeterRegistry());
    }

    private static long getLastEntryIdAsMillisSinceEpoch() {
        return lastEntryIdAsMillisSinceEpoch;
    }

    @Override
    @Transactional
    public void call(String lastEntry, List<DialogDataFraFeed> data) {
        if (!unleashService.isEnabled("veilarbdialog.kafka")) {
            log.info("DialogFeedDebug data: {}", data);
            try {
                data.forEach(info -> {
                    dialogFeedRepository.oppdaterDialogInfoForBruker(info);
                    CompletableFuture<Result<AktoerId>> future = elasticIndexer.indekserAsynkront(AktoerId.of(info.getAktorId()));

                    future.thenRun(() -> {
                        Duration timeElapsed = calculateTimeElapsed(info.getSisteEndring().toInstant());
                        MetricsFactory
                                .createEvent("portefolje.feed_time_elapsed_dialog")
                                .addFieldToReport("time_elapsed", timeElapsed)
                                .report();
                    });
                });

                Timestamp timestamp = timestampFromISO8601(lastEntry);
                lastEntryIdAsMillisSinceEpoch = timestamp.getTime();

                brukerRepository.updateMetadata(DIALOGAKTOR_SIST_OPPDATERT, Date.from(ZonedDateTime.parse(lastEntry).toInstant()));

                Event sistOppdatert = MetricsFactory.createEvent("portefolje.dialog.feed.sist.oppdatert");
                sistOppdatert.addFieldToReport("last_entry", lastEntry);
                sistOppdatert.report();

            } catch (Exception e) {
                String message = "Feil ved behandling av dialogdata fra feed for liste med brukere.";
                log.error(message, e);
            }
        }
    }
}
