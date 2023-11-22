package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;

import java.time.LocalDate;
import java.util.UUID;

public record HuskelappRedigerRequest(UUID huskelappId, Fnr brukerFnr, LocalDate frist, String kommentar, EnhetId enhetId) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappRedigerRequest {
    }
}
