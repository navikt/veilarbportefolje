package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;

import java.time.LocalDate;

public record HuskelappResponse(
        String huskelappId,
        Fnr brukerFnr,
        EnhetId enhetId,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate frist,
        String kommentar,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate endretDato,
        String endretAv) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappResponse {
    }
}
