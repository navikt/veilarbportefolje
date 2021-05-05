package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import no.nav.common.types.identer.AktorId;

import java.util.Optional;

public interface ArbeidslisteRepository {

    Optional<String> hentNavKontorForArbeidsliste(AktorId aktoerId);

    Try<Arbeidsliste> retrieveArbeidsliste(AktorId aktoerId);

    Try<ArbeidslisteDTO> insertArbeidsliste(ArbeidslisteDTO dto);

    Try<ArbeidslisteDTO> updateArbeidsliste(ArbeidslisteDTO data);

    int slettArbeidsliste(AktorId aktoerId);
}
