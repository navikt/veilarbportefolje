package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class OppfolgingPeriodeDTO {
    public ZonedDateTime startDato;
    public ZonedDateTime sluttDato;

    @java.beans.ConstructorProperties({"startDato", "sluttDato"})
    public OppfolgingPeriodeDTO(ZonedDateTime startDato, ZonedDateTime sluttDato) {
        this.startDato = startDato;
        this.sluttDato = sluttDato;
    }

    public OppfolgingPeriodeDTO() {
    }
}
