package no.nav.pto.veilarbportefolje.ensligforsorger.domain;

import java.time.LocalDate;

public record EnsligeForsorgerOvergangsstønadTiltak(Long vedtakid,
                                                    Periodetype vedtaksPeriodetype,
                                                    Aktivitetstype aktivitetsType,
                                                    LocalDate fra_dato, LocalDate til_dato) {
}
