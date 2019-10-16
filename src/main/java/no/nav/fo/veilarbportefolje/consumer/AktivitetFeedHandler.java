package no.nav.fo.veilarbportefolje.consumer;

import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.veilarbportefolje.service.AktivitetService;

import javax.inject.Inject;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;

@Slf4j
public class AktivitetFeedHandler implements FeedCallback<AktivitetDataFraFeed> {

    private final Counter antallTotaltMetrikk;
    private final Counter antallFeiletMetrikk;

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

        antallTotaltMetrikk = Counter.builder("portefolje_feed").tag("feed_name", "aktivitet").register(getMeterRegistry());
        antallFeiletMetrikk = Counter.builder("portefolje_feed_feilet").tag("feed_name", "aktivitet").register(getMeterRegistry());
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
        antallTotaltMetrikk.count();
        try {
            if (aktivitet.isHistorisk()) {
                aktivitetDAO.deleteById(aktivitet.getAktivitetId());
            } else {
                aktivitetDAO.upsertAktivitet(aktivitet);
            }
        } catch (Exception e) {
            String message = String.format("Kunne ikke lagre aktivitetdata fra feed for aktivitetid %s", aktivitet.getAktivitetId());
            log.error(message, e);
            antallFeiletMetrikk.count();
        }
    }

    void behandleAktivitetdata(List<AktoerId> aktoerids) {
        try {
            if (aktoerids.isEmpty()) {
                return;
            }
            aktivitetService.utledOgIndekserAktivitetstatuserForAktoerid(aktoerids);
        } catch (Exception e) {
            String message = "Feil ved behandling av aktivitetdata fra feed";
            log.error(message, e);
        }
    }
}