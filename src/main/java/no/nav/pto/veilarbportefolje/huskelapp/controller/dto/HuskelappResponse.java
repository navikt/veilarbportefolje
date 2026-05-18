package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;

import java.time.LocalDate;

public record HuskelappResponse(
        String huskelappId,
        Fnr brukerFnr,
        EnhetId enhetId,
        LocalDate frist,
        String kommentar,
        LocalDate endretDato,
        String endretAv) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappResponse {
    }
}
