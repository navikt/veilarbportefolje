package no.nav.pto.veilarbportefolje.huskelapp.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import no.nav.common.types.identer.Fnr;

import java.time.LocalDate;

public record HuskelappOpprettRequest(
        Fnr brukerFnr,
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate frist,
        String kommentar
) {
}
