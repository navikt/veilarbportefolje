package no.nav.pto.veilarbportefolje.huskeliste.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.common.types.identer.Fnr;

import java.time.LocalDate;

public record HuskelappOutputDto(String huskelappId, Fnr brukerFnr, LocalDate frist, String kommentar) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappOutputDto {
    }
}
