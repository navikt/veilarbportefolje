package no.nav.pto.veilarbportefolje.ensligforsorger.dto;

import java.time.LocalDate;

public record EnsligeForsorgerOvergangsstønadTiltakDto(
        String vedtaksPeriodetype,
        String aktivitetsType,
        LocalDate til_dato,
        LocalDate yngsteBarnsFødselsdato) {
}
