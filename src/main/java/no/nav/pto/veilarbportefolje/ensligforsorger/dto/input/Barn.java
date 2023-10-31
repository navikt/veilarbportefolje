package no.nav.pto.veilarbportefolje.ensligforsorger.dto.input;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.LocalDate;

public record Barn(
        String fødselsnummer,
        LocalDate termindato
) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Barn {
    }
}
