package no.nav.pto.veilarbportefolje.domene;

import java.time.LocalDate;

public record EnsligeForsorgereOvergangsstonad(
        String vedtaksPeriodetype,
        Boolean aktivitetsplikt,
        LocalDate til_dato,
        LocalDate yngsteBarnsFødselsdato) {

}
