package no.nav.pto.veilarbportefolje.ensligforsorger.dto.input;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Aktivitetstype;
import no.nav.pto.veilarbportefolje.ensligforsorger.domain.Periodetype;

import java.time.LocalDate;

public record Periode(
        LocalDate fom,
        LocalDate tom,
        Periodetype periodetype,
        Aktivitetstype aktivitetstype
) {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Periode {
    }
}
