package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class Arbeidsliste {
    boolean isArbeidsliste;
    String veilederId;
    Timestamp endringstidspunkt;
    String kommentar;
    Timestamp frist;
    boolean isVeilederOppfolgendeVeileder;
}
