package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import no.nav.common.types.identer.Fnr;

import java.time.LocalDate;
import java.util.UUID;

public record HuskelappRedigerRequest(
        UUID huskelappId,
        Fnr brukerFnr,
        @JsonSerialize(using = LocalDateSerializer.class)
        LocalDate frist,
        String kommentar
) {
}
