package no.nav.pto.veilarbportefolje.aktiviteter.domene;


import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class Aktivitet {
    public String aktivitetID;
    private String aktivitetType;
    private String status;
    private Timestamp fraDato;
    private Timestamp tilDato;

}
