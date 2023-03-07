package no.nav.pto.veilarbportefolje.ensligforsorger.dto.input;

import java.time.LocalDate;

public record Barn(
        String fødselsnummer,
        LocalDate termindato
) {
}
