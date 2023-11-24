package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.huskelapp.domain.HuskelappStatus;

import java.sql.Timestamp;
import java.util.UUID;

public record HuskelappOutputDto(UUID huskelappId, Fnr brukerFnr, EnhetId enhetID, Timestamp frist, String kommentar, HuskelappStatus huskelappStatus) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappOutputDto {
    }
}
