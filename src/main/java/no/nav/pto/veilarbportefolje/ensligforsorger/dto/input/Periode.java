package no.nav.pto.veilarbportefolje.ensligforsorger.dto.input;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public record Periode(
        LocalDate fom,
        LocalDate tom,
        Periodetype periodetype,
        Aktivitetstype aktivitetstype
) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Periode {
    }
}
