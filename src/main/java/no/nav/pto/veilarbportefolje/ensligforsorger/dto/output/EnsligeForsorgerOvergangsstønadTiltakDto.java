package no.nav.pto.veilarbportefolje.ensligforsorger.dto.output;

import java.time.LocalDate;

public record EnsligeForsorgerOvergangsstønadTiltakDto(
        String vedtaksPeriodetypeBeskrivelse,
        Boolean aktivitsplikt,
        LocalDate utløpsDato,
        LocalDate yngsteBarnsFødselsdato) {
}
