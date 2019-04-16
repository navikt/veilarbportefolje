package no.nav.fo.veilarbportefolje.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.veilarbportefolje.service.AktivitetService;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbportefolje.consumer.AsyncAwaitUtils.async;
import static no.nav.fo.veilarbportefolje.consumer.AsyncAwaitUtils.await;

@Slf4j
public class AktivitetFeedHandler implements FeedCallback<AktivitetDataFraFeed> {

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
    }

    @Override
    public void call(String lastEntry, List<AktivitetDataFraFeed> data) {

        List<AktivitetDataFraFeed> avtalteAktiviteter = data
                .stream()
                .filter(AktivitetDataFraFeed::isAvtalt)
                .collect(toList());

        List<Runnable> jobs = avtalteAktiviteter.stream().map(this::toRunnable).collect(toList());
        CompletableFuture future = async(jobs);
        await(future);

        List<AktoerId> aktoerIds = avtalteAktiviteter
                .stream().map(AktivitetDataFraFeed::getAktorId)
                .distinct()
                .map(AktoerId::of)
                .collect(toList());

        CompletableFuture f = async(() -> behandleAktivitetdata(aktoerIds));
        await(f);

        brukerRepository.setAktiviteterSistOppdatert(lastEntry);
    }

    private Runnable toRunnable(AktivitetDataFraFeed aktivitet) {
        return () -> {
            if (aktivitet.isHistorisk()) {
                aktivitetDAO.deleteById(aktivitet.getAktivitetId());
            } else {
                aktivitetDAO.upsertAktivitet(aktivitet);
            }
        };
    }

    void behandleAktivitetdata(List<AktoerId> aktoerids) {
        if (aktoerids.isEmpty()) {
            return;
        }
        aktivitetService.utledOgIndekserAktivitetstatuserForAktoerid(aktoerids);
    }
}
