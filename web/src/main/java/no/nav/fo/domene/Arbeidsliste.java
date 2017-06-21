package no.nav.fo.domene;

import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
@Getter
public class Arbeidsliste {
    final String veilederId;
    final Timestamp endringstidspunkt;
    final String kommentar;
    final Timestamp frist;
    boolean isOppfolgendeVeileder;
}
