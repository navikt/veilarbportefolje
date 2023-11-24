package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;

import java.sql.Timestamp;
import java.util.UUID;

public record HuskelappOutputDto(UUID huskelappId, Fnr brukerFnr, EnhetId enhetID, Timestamp frist, String kommentar) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappOutputDto {
    }
}
