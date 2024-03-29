package no.nav.pto.veilarbportefolje.arbeidsliste.v1;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ArbeidslisteRequest {
    String fnr;
    String overskrift;
    String kommentar;
    String frist;
    String kategori;
}
