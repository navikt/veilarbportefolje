package no.nav.fo.provider.rest.arbeidsliste;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ArbeidslisteRequest {
    String fnr;
    String veilederId;
    String kommentar;
    String frist;
}
