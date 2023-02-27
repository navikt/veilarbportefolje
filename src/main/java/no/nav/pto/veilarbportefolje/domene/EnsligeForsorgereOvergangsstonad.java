package no.nav.pto.veilarbportefolje.domene;

import java.time.LocalDate;

public record EnsligeForsorgereOvergangsstonad(
        String vedtaksPeriodetype,
        String aktivitetsType,
        LocalDate til_dato,
        LocalDate yngsteBarnsFødselsdato) {

}
