package no.nav.pto.veilarbportefolje.ensligforsorger.domain;

import java.time.LocalDate;

public record EnsligeForsorgerOvergangsstønadTiltak(Long vedtakid,
                                                    String vedtaksPeriodetype,
                                                    String aktivitetsType,
                                                    LocalDate fra_dato, LocalDate til_dato) {
}
