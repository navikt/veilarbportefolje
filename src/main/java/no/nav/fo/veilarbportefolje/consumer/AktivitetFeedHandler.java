package no.nav.fo.veilarbportefolje.consumer;

import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.veilarbportefolje.service.AktivitetService;
import no.nav.metrics.MetricsFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

@Slf4j
public class AktivitetFeedHandler implements FeedCallback<AktivitetDataFraFeed> {

    private final Counter counter;
    private BrukerRepository brukerRepository;
    private AktivitetService aktivitetService;
    private AktivitetDAO aktivitetDAO;

    @Inject
    public AktivitetFeedHandler(BrukerRepository brukerRepository,
                                AktivitetService aktivitetService,
                                AktivitetDAO aktivitetDAO) {
        this.brukerRepository = brukerRepository;
        this.aktivitetService = aktivitetService;
        this.aktivitetDAO = aktivitetDAO;

        this.counter = Counter.builder("portefolje_feed_aktiviteter").register(MetricsFactory.getMeterRegistry());
    }

    @Override
    public void call(String lastEntry, List<AktivitetDataFraFeed> data) {
        List<AktivitetDataFraFeed> avtalteAktiviteter = data
                .stream()
                .filter(AktivitetDataFraFeed::isAvtalt)
                .collect(toList());

        avtalteAktiviteter.forEach(this::lagreAktivitetData);

        behandleAktivitetdata(avtalteAktiviteter
                .stream().map(AktivitetDataFraFeed::getAktorId)
                .distinct()
                .map(AktoerId::of)
                .collect(toList()));


        brukerRepository.setAktiviteterSistOppdatert(lastEntry);
    }

    private void lagreAktivitetData(AktivitetDataFraFeed aktivitet) {
        try {
            if (aktivitet.isHistorisk()) {
                aktivitetDAO.deleteById(aktivitet.getAktivitetId());
            } else {
                aktivitetDAO.upsertAktivitet(aktivitet);
            }
        } catch (Exception e) {
            log.error("Kunne ikke lagre aktivitetdata fra feed for aktivitetid {}.", aktivitet.getAktivitetId(), e);
            throw e;
        }
        counter.increment();
    }

    void behandleAktivitetdata(List<AktoerId> aktoerids) {
        try {
            if (aktoerids.isEmpty()) {
                return;
            }
            aktivitetService.utledOgIndekserAktivitetstatuserForAktoerid(aktoerids);
        } catch (Exception e) {
            log.error("Feil ved behandling av aktivitetdata fra feed", e);
            throw e;
        }
    }
}
