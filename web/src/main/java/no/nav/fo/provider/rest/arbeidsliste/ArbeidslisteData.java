package no.nav.fo.provider.rest.arbeidsliste;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class ArbeidslisteData {
    final Fnr fnr;
    AktoerId aktoerId;
    String veilederId;
    String kommentar;
    Timestamp frist;
}
