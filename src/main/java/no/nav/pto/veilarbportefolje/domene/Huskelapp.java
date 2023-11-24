package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.sql.Timestamp;

public record Huskelapp(String kommentar, Timestamp frist) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Huskelapp {
    }
}
