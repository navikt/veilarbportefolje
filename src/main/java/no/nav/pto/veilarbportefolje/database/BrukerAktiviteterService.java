package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.GruppeAktivitetRepository;
import no.nav.pto.veilarbportefolje.arenapakafka.aktiviteter.TiltakRepositoryV1;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrukerAktiviteterService {
    private final AktivitetService aktivitetService;
    private final TiltakRepositoryV1 tiltakRepositoryV1;
    private final OppfolgingRepository oppfolgingRepository;
    private final GruppeAktivitetRepository gruppeAktivitetRepository;
    private final BrukerService brukerService;
    private final OpensearchIndexer opensearchIndexer;

    public void syncAktivitetOgBrukerData() {
        log.info("Starter jobb: oppdater BrukerAktiviteter og BrukerData");
        List<AktorId> brukereSomMaOppdateres = oppfolgingRepository.hentAlleGyldigeBrukereUnderOppfolging();
        log.info("Oppdaterer brukerdata for alle brukere under oppfolging: {}", brukereSomMaOppdateres.size());
        deaktiverUtgatteUtdanningsAktivteterPostgres();
        syncAktivitetOgBrukerData(brukereSomMaOppdateres);
        log.info("Avslutter jobb: oppdater BrukerAktiviteter og BrukerData");
        opensearchIndexer.nyHovedIndeksering(brukereSomMaOppdateres);
    }


    public void syncAktivitetOgBrukerData(List<AktorId> brukere) {
        brukere.forEach(aktorId -> {
            log.info("Oppdater BrukerAktiviteter og BrukerData for aktorId: {}", aktorId);
                    if (aktorId != null) {
                        try {
                            PersonId personId = brukerService.hentPersonidFraAktoerid(aktorId).toJavaOptional().orElse(null);
                            syncAktiviteterOgBrukerData(personId, aktorId);
                        } catch (Exception e) {
                            log.warn("Fikk error under sync jobb, men fortsetter. Aktoer: {}, exception: {}", aktorId, e);
                        }
                    }
                }
        );
    }

    public void syncAktivitetOgBrukerData(AktorId aktorId) {
        PersonId personId = brukerService.hentPersonidFraAktoerid(aktorId).toJavaOptional().orElse(null);
        if (personId == null) {
            log.info("Fant ingen personId pa aktor: {}", aktorId);
        }
        syncAktiviteterOgBrukerData(personId, aktorId);
        opensearchIndexer.indekser(aktorId);
    }

    public void syncAktiviteterOgBrukerData(PersonId personId, AktorId aktorId) {
        if(personId == null){
            // TODO: check om utdatert
            log.warn("AktoerId ble ikke oppdatert da personId er null: {}. Inaktiv aktorId?", aktorId.get());
            return;
        }
        aktivitetService.deaktiverUtgatteUtdanningsAktivteter(aktorId);

        tiltakRepositoryV1.utledOgLagreTiltakInformasjon(aktorId, personId);
        gruppeAktivitetRepository.utledOgLagreGruppeaktiviteter(aktorId, personId);
        aktivitetService.utledAktivitetstatuserForAktoerid(aktorId);
    }


    public void deaktiverUtgatteUtdanningsAktivteterPostgres() {
        aktivitetService.deaktiverUtgatteUtdanningsAktivteterPostgres();
    }
}
