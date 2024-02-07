package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;

import java.time.LocalDate;

public record HuskelappResponse(
        String huskelappId,
        Fnr brukerFnr,
        EnhetId enhetId,
        @JsonDeserialize(using = LocalDateDeserializer.class)
        LocalDate frist,
        String kommentar,
        @JsonDeserialize(using = LocalDateDeserializer.class)
        LocalDate endretDato,
        String endretAv) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappResponse {
    }
}
