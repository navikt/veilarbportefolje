package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.GruppeAktivitetRepository;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.springframework.stereotype.Service;

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
        if(personId == null){
            log.info("Fant ingen personId pa aktor: {}", aktorId);
        }
        tiltakRepositoryV2.utledOgLagreTiltakInformasjon(personId, aktorId);
        gruppeAktivitetRepository.utledOgLagreGruppeaktiviteter(personId, aktorId);
        aktivitetService.utledOgIndekserAktivitetstatuserForAktoerid(aktorId);

        brukerDataService.oppdaterAktivitetBrukerData(aktorId);
    }
}
