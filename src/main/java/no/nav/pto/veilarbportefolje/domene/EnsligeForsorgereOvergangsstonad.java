package no.nav.pto.veilarbportefolje.domene;

import java.time.LocalDate;

public record EnsligeForsorgereOvergangsstonad(
        String vedtaksPeriodetype,
        Boolean harAktivitetsplikt,
        LocalDate utlopsDato,
        LocalDate yngsteBarnsFødselsdato) {

}
