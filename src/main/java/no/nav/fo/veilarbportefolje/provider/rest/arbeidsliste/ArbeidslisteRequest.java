package no.nav.fo.veilarbportefolje.provider.rest.arbeidsliste;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ArbeidslisteRequest {
    String fnr;
    String overskrift;
    String kommentar;
    String frist;
}
