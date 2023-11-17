package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public record Huskelapp(String kommentar, LocalDate frist) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Huskelapp {
    }
}
