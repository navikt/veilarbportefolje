package no.nav.fo.veilarbportefolje.provider.rest.arbeidsliste;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.auth.SubjectHandler;
import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.Fnr;
import no.nav.fo.veilarbportefolje.domene.VeilederId;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class ArbeidslisteData {
    final Fnr fnr;
    AktoerId aktoerId;
    VeilederId veilederId;
    String overskrift;
    String kommentar;
    Timestamp frist;
    Timestamp endringstidspunkt;
    Boolean isOppfolgendeVeileder;

    public static ArbeidslisteData of(Fnr fnr, String overskrift, String kommentar, Timestamp frist) {
        return
                new ArbeidslisteData(fnr)
                        .setVeilederId(VeilederId.of(SubjectHandler.getIdent().orElseThrow(IllegalStateException::new)))
                        .setOverskrift(overskrift)
                        .setKommentar(kommentar)
                        .setFrist(frist);
    }
}
