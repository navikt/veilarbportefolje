package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public record HuskelappOpprettResponse(String huskelappId) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappOpprettResponse {
    }
}
