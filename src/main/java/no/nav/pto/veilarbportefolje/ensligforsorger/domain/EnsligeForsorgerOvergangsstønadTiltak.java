package no.nav.pto.veilarbportefolje.ensligforsorger.domain;

import no.nav.common.types.identer.Fnr;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EnsligeForsorgerOvergangsstønadTiltak(
        Fnr personIdent,
        Long vedtakid,
        Periodetype vedtaksPeriodetype,
        Aktivitetstype aktivitetsType,
        LocalDate fra_dato,
        LocalDate til_dato,
        LocalDateTime radSistEndret
) {
}
