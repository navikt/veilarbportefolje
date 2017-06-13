package no.nav.fo.provider.rest.arbeidsliste;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class ArbeidslisteResponse {
    boolean isArbeidsliste;
    String veilederId;
    Timestamp endringstidspunkt;
    String beskrivelse;
    Timestamp frist;
    boolean isVeilederOppfolgendeVeileder;
}
