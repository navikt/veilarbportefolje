package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class OppfolgingPeriodeDTO {
    public LocalDateTime startDato;
    public LocalDateTime sluttDato;

    @java.beans.ConstructorProperties({"startDato", "sluttDato"})
    public OppfolgingPeriodeDTO(LocalDateTime startDato, LocalDateTime sluttDato) {
        this.startDato = startDato;
        this.sluttDato = sluttDato;
    }

    public OppfolgingPeriodeDTO() {
    }
}
