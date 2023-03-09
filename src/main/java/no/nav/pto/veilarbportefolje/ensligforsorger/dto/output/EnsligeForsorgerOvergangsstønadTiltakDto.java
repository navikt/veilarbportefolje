package no.nav.pto.veilarbportefolje.ensligforsorger.dto.output;

import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad;

import java.time.LocalDate;

public record EnsligeForsorgerOvergangsstønadTiltakDto(
        String vedtaksPeriodetypeBeskrivelse,
        Boolean aktivitsplikt,
        LocalDate utløpsDato,
        LocalDate yngsteBarnsFødselsdato) {

    public EnsligeForsorgereOvergangsstonad toEnsligeForsorgereOpensearchDto() {
        return new EnsligeForsorgereOvergangsstonad(vedtaksPeriodetypeBeskrivelse, aktivitsplikt, utløpsDato, yngsteBarnsFødselsdato);
    }
}
