package no.nav.pto.veilarbportefolje.dialog;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class DialogData  {
    public String aktorId;
    public Date sisteEndring;
    public Date tidspunktEldsteVentende;
    public Date tidspunktEldsteUbehandlede;
}
