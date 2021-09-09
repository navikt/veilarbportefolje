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
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.util.BatchConsumer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static no.nav.pto.veilarbportefolje.util.BatchConsumer.batchConsumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrukerAktiviteterService {
    private final AktivitetService aktivitetService;
    private final TiltakRepositoryV2 tiltakRepositoryV2;
    private final OppfolgingRepository oppfolgingRepository;
    private final GruppeAktivitetRepository gruppeAktivitetRepository;
    private final BrukerService brukerService;

    public void syncAktivitetOgBrukerData() {
        log.info("Starter jobb: oppdater BrukerAktiviteter og BrukerData");
        List<AktorId> brukereSomMaOppdateres = oppfolgingRepository.hentAlleBrukereUnderOppfolging();
        log.info("Oppdaterer brukerdata for alle brukere under oppfolging: {}", brukereSomMaOppdateres.size());
        syncAktivitetOgBrukerData(brukereSomMaOppdateres);
        log.info("Avslutter jobb: oppdater BrukerAktiviteter og BrukerData");
    }

    public void syncAktivitetOgBrukerData(List<AktorId> brukere) {
        ForkJoinPool pool = new ForkJoinPool(4); // Prøv et annet tall hvis dette ikke fungerer, mellom 2 og 10
        try {
            pool.submit(() ->
                    brukere.parallelStream().forEach(aktorId -> {
                        log.info("Start tråd: oppdater BrukerAktiviteter og BrukerData");
                                if (aktorId != null) {
                                    try {
                                        PersonId personId = brukerService.hentPersonidFraAktoerid(aktorId).toJavaOptional().orElse(null);
                                        syncAktiviteterOgBrukerData(personId, aktorId);
                                    } catch (Exception e) {
                                        log.warn("Fikk error under sync jobb, men fortsetter aktoer: {}, exception: {}", aktorId, e);
                                    }
                                }
                                log.info("Avslutter tråd: oppdater BrukerAktiviteter og BrukerData");
                            }
                    )).get(5, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("Error i sync jobben.", e);
        }
    }

    public void syncAktivitetOgBrukerData(AktorId aktorId) {
        PersonId personId = brukerService.hentPersonidFraAktoerid(aktorId).toJavaOptional().orElse(null);
        if (personId == null) {
            log.info("Fant ingen personId pa aktor: {}", aktorId);
        }
        syncAktiviteterOgBrukerData(personId, aktorId);
    }

    public void syncAktiviteterOgBrukerData(PersonId personId, AktorId aktorId) {
        if(personId == null){
            // TODO: check om utdatert
            log.warn("AktoerId ble ikke oppdatert da personId er null: {}. Inaktiv aktorId?", aktorId.get());
            return;
        }
        tiltakRepositoryV2.utledOgLagreTiltakInformasjon(aktorId, personId);
        gruppeAktivitetRepository.utledOgLagreGruppeaktiviteter(aktorId, personId);
        aktivitetService.deaktiverUtgatteUtdanningsAktivteter(aktorId);
        aktivitetService.utledOgIndekserAktivitetstatuserForAktoerid(aktorId);
    }
}
