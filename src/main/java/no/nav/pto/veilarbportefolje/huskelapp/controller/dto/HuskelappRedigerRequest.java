package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;

import java.sql.Timestamp;
import java.util.UUID;

public record HuskelappRedigerRequest(UUID huskelappId, Fnr brukerFnr, Timestamp frist, String kommentar,
                                      EnhetId enhetId) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappRedigerRequest {
    }
}
