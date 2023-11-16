package no.nav.pto.veilarbportefolje.huskeliste.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;

import java.time.LocalDate;

public record HuskelappInputDto(Fnr brukerFnr, LocalDate frist, String kommentar, EnhetId enhetId) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappInputDto {
    }
}
