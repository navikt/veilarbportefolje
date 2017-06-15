package no.nav.fo.provider.rest.arbeidsliste;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.fo.domene.Fnr;

import java.sql.Timestamp;

/**
 * TODO: Kan vi lage en immutable value type som returnere en kopi ved setX()?
 */
@Data
@Accessors(chain = true)
public class ArbeidslisteUpdate {
    final Fnr fnr;
    String aktoerID;
    String veilederId;
    String kommentar;
    Timestamp frist;
}
