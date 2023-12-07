package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public record HuskelappForBruker(
    LocalDate frist,
    String kommentar
) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public HuskelappForBruker {
    }
}
