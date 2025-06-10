package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDateTime;

public record Utkast14a(
        String status,
        LocalDateTime statusEndret,
        String ansvarligVeileder
) {
    public static Utkast14a of(String status, LocalDateTime statusEndret, String ansvarligVeileder) {
        if (status != null || statusEndret != null || ansvarligVeileder != null) {
            return new Utkast14a(status, statusEndret, ansvarligVeileder);
        } else {
            return null;
        }
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Utkast14a {
    }
}
