package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class Arbeidsliste {
    final String veilederId;
    final Timestamp endringstidspunkt;
    final String kommentar;
    final Timestamp frist;
    boolean isOppfolgendeVeileder;
}
