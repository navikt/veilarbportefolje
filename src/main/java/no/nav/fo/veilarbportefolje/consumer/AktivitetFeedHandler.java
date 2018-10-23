package no.nav.fo.veilarbportefolje.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.veilarbportefolje.service.AktivitetService;
import org.apache.commons.codec.digest.DigestUtils;

import javax.inject.Inject;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbportefolje.util.MetricsUtils.timed;

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
            timed(
                    "feed.aktivitet.objekt",
                    () -> {
                        if (aktivitet.isHistorisk()) {
                            aktivitetDAO.deleteById(aktivitet.getAktivitetId());
                        } else {
                            aktivitetDAO.upsertAktivitet(aktivitet);
                        }
                    },
                    (timer, hasFailed) -> {
                        if (hasFailed) {
                            timer.addTagToReport("aktoerhash", DigestUtils.md5Hex(aktivitet.getAktorId()).toUpperCase());
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Kunne ikke lagre aktivitetdata fra feed for aktivitetid {}.", aktivitet.getAktivitetId(), e);
        }
    }

    void behandleAktivitetdata(List<AktoerId> aktoerids) {
        try {
            timed(
                    "feed.aktivitet.indekseraktivitet",
                    () -> {
                        if (aktoerids.isEmpty()) {
                            return;
                        }
                        aktivitetService.utledOgIndekserAktivitetstatuserForAktoerid(aktoerids);
                    }, (timer, hasFailed) -> timer.addTagToReport("antall", Integer.toString(aktoerids.size()))

            );
        } catch (Exception e) {
            log.error("Feil ved behandling av aktivitetdata fra feed", e);
        }
    }
}
