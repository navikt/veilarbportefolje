package no.nav.fo.provider.rest.arbeidsliste;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class ArbeidslisteRequest {
    String kommentar;
    Timestamp frist;
}
