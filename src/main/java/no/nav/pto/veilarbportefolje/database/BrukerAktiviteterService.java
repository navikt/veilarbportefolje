package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.GruppeAktivitetRepository;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrukerAktiviteterService {
    private final AktivitetService aktivitetService;
    private final TiltakRepositoryV2 tiltakRepositoryV2;
    private final GruppeAktivitetRepository gruppeAktivitetRepository;
    private final BrukerService brukerService;
    private final BrukerDataService brukerDataService;

    public void syncAktivitetOgBrukerData(AktorId aktorId) {
        PersonId personId = brukerService.hentPersonidFraAktoerid(aktorId).toJavaOptional().orElse(null);
        if (personId == null) {
            log.info("Fant ingen personId pa aktor: {}", aktorId);
        }
        syncAktiviteterOgBrukerData(personId, aktorId);
    }

    public void syncAktivitetOgBrukerData() {
        List<OppfolgingsBruker> oppfolgingsBruker = brukerService.hentAlleBrukereUnderOppfolging();
        log.info("Starter jobb: sync aktiviteter og brukerdata for {} brukere", oppfolgingsBruker.size());
        oppfolgingsBruker.forEach(bruker -> {
                    if (bruker.getAktoer_id() != null && bruker.getAktoer_id() != null) {
                        try {
                            syncAktiviteterOgBrukerData(PersonId.of(bruker.getPerson_id()), AktorId.of(bruker.getAktoer_id()));
                        } catch (Exception e) {
                            log.warn("Fikk error under sync jobb, men fortsetter aktoer: {}, exception: {}", bruker.getAktoer_id(), e);
                        }
                    } else {
                        log.info("Fant ikke baade aktoerId: {} og personId: {}", bruker.getAktoer_id(), bruker.getAktoer_id());
                    }
                }
        );
    }

    public void syncAktivitetOgBrukerData(List<AktorId> brukere) {
        ForkJoinPool pool = new ForkJoinPool(8);
        try {
            pool.submit(() ->
                    brukere.parallelStream().forEach(aktorId -> {
                                if (aktorId != null) {
                                    try {
                                        PersonId personId = brukerService.hentPersonidFraAktoerid(aktorId).toJavaOptional().orElse(null);
                                        syncAktiviteterOgBrukerData(personId, aktorId);
                                    } catch (Exception e) {
                                        log.warn("Fikk error under sync jobb, men fortsetter aktoer: {}, exception: {}", aktorId, e);
                                    }
                                }
                            }
                    )).get(30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Error i sync jobben.", e);
        }
    }

    private void syncAktiviteterOgBrukerData(PersonId personId, AktorId aktorId) {
        tiltakRepositoryV2.utledOgLagreTiltakInformasjon(personId, aktorId);
        gruppeAktivitetRepository.utledOgLagreGruppeaktiviteter(personId, aktorId);
        aktivitetService.utledOgIndekserAktivitetstatuserForAktoerid(aktorId);

        brukerDataService.oppdaterAktivitetBrukerData(aktorId);
    }
}
