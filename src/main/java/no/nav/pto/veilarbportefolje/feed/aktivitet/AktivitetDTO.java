package no.nav.pto.veilarbportefolje.feed.aktivitet;


import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class AktivitetDTO {
    private String aktivitetType;
    private String status;
    private Timestamp fraDato;
    private Timestamp tilDato;

}
