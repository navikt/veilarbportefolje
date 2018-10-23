package no.nav.fo.veilarbportefolje.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data()
@Accessors(chain = true)
public class KrrDAO {
    String fnr;
    String reservertIKrr;
    Timestamp sistVerifisert;
    Timestamp lagtTilIDB;
}
