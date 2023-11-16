package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Date;

public record Huskelapp(String id, String kommentar, Date frist, Date opprettet_dato) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Huskelapp {
    }
}
