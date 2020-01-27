package no.nav.pto.veilarbportefolje.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data()
@Accessors(chain = true)
public class KrrDTO {
    String fnr;
    String reservertIKrr;
    Timestamp sistVerifisert;
    Timestamp lagtTilIDB;
}
