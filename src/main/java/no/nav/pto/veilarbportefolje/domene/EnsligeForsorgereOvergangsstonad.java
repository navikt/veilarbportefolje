package no.nav.pto.veilarbportefolje.domene;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public record EnsligeForsorgereOvergangsstonad(
        String vedtaksPeriodetype,
        Boolean harAktivitetsplikt,
        LocalDate utlopsDato,
        LocalDate yngsteBarnsFødselsdato) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public EnsligeForsorgereOvergangsstonad {
    }

}
