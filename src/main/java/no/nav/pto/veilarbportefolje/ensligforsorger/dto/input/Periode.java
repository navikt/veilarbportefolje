package no.nav.pto.veilarbportefolje.ensligforsorger.dto.input;

import java.time.LocalDate;

public record Periode(
        LocalDate fom,
        LocalDate tom,
        Periodetype periodetype,
        Aktivitetstype aktivitetstype
) {
}
