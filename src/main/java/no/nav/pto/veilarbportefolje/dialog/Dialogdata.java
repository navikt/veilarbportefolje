package no.nav.pto.veilarbportefolje.dialog;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.util.Date;

@Data
@Accessors(chain = true)
public class Dialogdata {
    public String aktorId;
    public ZonedDateTime sisteEndring;
    public ZonedDateTime tidspunktEldsteVentende;
    public ZonedDateTime tidspunktEldsteUbehandlede;
}
