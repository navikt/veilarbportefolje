package no.nav.fo.provider.rest.arbeidsliste;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.brukerdialog.security.context.SubjectHandler;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.VeilederId;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class ArbeidslisteData {
    final Fnr fnr;
    AktoerId aktoerId;
    VeilederId veilederId;
    String kommentar;
    Timestamp frist;
    Timestamp endringstidspunkt;
    Boolean isOppfolgendeVeileder;

    public static ArbeidslisteData of(Fnr fnr, String kommentar, Timestamp frist) {
        return
                new ArbeidslisteData(fnr)
                        .setVeilederId(new VeilederId(SubjectHandler.getSubjectHandler().getUid()))
                        .setKommentar(kommentar)
                        .setFrist(frist);
    }
}
