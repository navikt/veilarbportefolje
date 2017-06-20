package no.nav.fo.domene;

import lombok.Value;
import no.nav.fo.provider.rest.arbeidsliste.ArbeidslisteData;

import java.sql.Timestamp;

@Value
public class Arbeidsliste {
    String veilederId;
    Timestamp endringstidspunkt;
    String kommentar;
    Timestamp frist;
    boolean isOppfolgendeVeileder;

    public static Arbeidsliste of(ArbeidslisteData data) {
        return new Arbeidsliste(
                data.getVeilederId(),
                data.getEndringstidspunkt(),
                data.getKommentar(),
                data.getFrist(),
                data.getIsOppfolgendeVeileder()
        );
    }
}
