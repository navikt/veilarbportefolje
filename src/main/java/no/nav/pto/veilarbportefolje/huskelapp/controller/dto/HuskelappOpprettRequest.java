package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;

import java.sql.Timestamp;

public record HuskelappOpprettRequest(Fnr brukerFnr, Timestamp frist, String kommentar, EnhetId enhetId) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappOpprettRequest {
    }
}
