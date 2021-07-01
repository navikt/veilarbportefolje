package no.nav.pto.veilarbportefolje.database;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.arenaaktiviteter.TiltakRepositoryV2;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrukerDataService {
    private final AktivitetDAO aktivitetDAO;
    private final TiltakRepositoryV2 tiltakRepositoryV2;
    private final BrukerDataRepository brukerDataRepository;
    public void oppdaterAktivitetBrukerData(AktorId aktorId, PersonId personId){

    }
}
