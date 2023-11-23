package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;

import java.time.LocalDate;

public record HuskelappOutputDto(String huskelappId, Fnr brukerFnr, EnhetId enhetID, LocalDate frist, String kommentar) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappOutputDto {
    }
}
