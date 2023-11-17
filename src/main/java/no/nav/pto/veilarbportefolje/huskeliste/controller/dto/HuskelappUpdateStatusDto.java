package no.nav.pto.veilarbportefolje.huskeliste.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.pto.veilarbportefolje.huskeliste.HuskelappStatus;

public record HuskelappUpdateStatusDto(String huskelappId, HuskelappStatus status) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappUpdateStatusDto {
    }
}
