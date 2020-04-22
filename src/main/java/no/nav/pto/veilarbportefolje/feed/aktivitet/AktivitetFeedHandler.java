package no.nav.pto.veilarbportefolje.feed.aktivitet;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.metrics.MetricsFactory;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;
import static no.nav.pto.veilarbportefolje.util.DateUtils.*;

@Slf4j
public class AktivitetFeedHandler implements FeedCallback<AktivitetDataFraFeed> {

    private static final String FEED_NAME = "aktivitet";
    private final Counter antallTotaltMetrikk;

    private BrukerRepository brukerRepository;
    private AktivitetService aktivitetService;
    private AktivitetDAO aktivitetDAO;

    private static long lastEntryIdAsMillisSinceEpoch;

    private static Map<String, Instant> endretTidspunktForAktoerId = new HashMap<>();

    @Inject
    public AktivitetFeedHandler(BrukerRepository brukerRepository,
                                AktivitetService aktivitetService,
                                AktivitetDAO aktivitetDAO) {
        this.brukerRepository = brukerRepository;
        this.aktivitetService = aktivitetService;
        this.aktivitetDAO = aktivitetDAO;

        antallTotaltMetrikk = Counter.builder("portefolje_feed").tag("feed_name", FEED_NAME).register(getMeterRegistry());
        Gauge.builder("portefolje_feed_last_id", AktivitetFeedHandler::getLastEntryIdAsMillisSinceEpoch).tag("feed_name", FEED_NAME).register(getMeterRegistry());
    }

    private static long getLastEntryIdAsMillisSinceEpoch() {
        return lastEntryIdAsMillisSinceEpoch;
    }

    @Override
    public void call(String lastEntry, List<AktivitetDataFraFeed> data) {

        log.info("AktivitetfeedDebug data: {}", data);

        List<AktivitetDataFraFeed> avtalteAktiviteter = data
                .stream()
                .filter(AktivitetDataFraFeed::isAvtalt)
                .collect(toList());

        endretTidspunktForAktoerId = new HashMap<>();
        avtalteAktiviteter.forEach(this::lagreAktivitetData);

        behandleAktivitetdata(avtalteAktiviteter
                                      .stream().map(AktivitetDataFraFeed::getAktorId)
                                      .distinct()
                                      .map(AktoerId::of)
                                      .collect(toList()));

        Timestamp lastEntryId = timestampFromISO8601(lastEntry);
        lastEntryIdAsMillisSinceEpoch = lastEntryId.getTime();
        brukerRepository.setAktiviteterSistOppdatert(lastEntryId);
    }

    private void lagreAktivitetData(AktivitetDataFraFeed aktivitet) {

        endretTidspunktForAktoerId.put(aktivitet.getAktorId(), aktivitet.endretDato.toInstant());

        try {
            if (aktivitet.isHistorisk()) {
                aktivitetDAO.deleteById(aktivitet.getAktivitetId());
            } else {
                aktivitetDAO.upsertAktivitet(aktivitet);
            }
        } catch (Exception e) {
            String message = String.format("Kunne ikke lagre aktivitetdata fra feed for aktivitetid %s", aktivitet.getAktivitetId());
            log.error(message, e);
        }
    }

    void behandleAktivitetdata(List<AktoerId> aktoerids) {
        try {
            antallTotaltMetrikk.increment(aktoerids.size());
            if (aktoerids.isEmpty()) {
                return;
            }
            CompletableFuture<Void> indeksering = aktivitetService.utledOgIndekserAktivitetstatuserForAktoerid(aktoerids);
            indeksering.thenRun(() -> {
                aktoerids.forEach(aktoerId -> {
                    Duration timeElapsed = calculateTimeElapsed(endretTidspunktForAktoerId.get(aktoerId.toString()));
                    MetricsFactory
                            .createEvent("portefolje.feed_time_elapsed_aktivitet")
                            .addFieldToReport("time_elapsed", timeElapsed.toMillis())
                            .report();
                });
            });
        } catch (Exception e) {
            String message = "Feil ved behandling av aktivitetdata fra feed";
            log.error(message, e);
        }
    }
}
