package no.nav.pto.veilarbportefolje.huskelapp.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;

import java.time.LocalDate;
import java.util.UUID;

public record Huskelapp(
        UUID huskelappId,
        Fnr brukerFnr,
        EnhetId enhetId,
        VeilederId endretAv,
        LocalDate endretDato,
        LocalDate frist,
        String kommentar,
        HuskelappStatus status

) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Huskelapp {
    }

}
