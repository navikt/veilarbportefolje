package no.nav.pto.veilarbportefolje.arbeidsliste;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.auth.SubjectHandler;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class ArbeidslisteDTO {
    final Fnr fnr;
    AktoerId aktoerId;
    VeilederId veilederId;
    String overskrift;
    String kommentar;
    Timestamp frist;
    Timestamp endringstidspunkt;
    Boolean isOppfolgendeVeileder;

    public static ArbeidslisteDTO of(Fnr fnr, String overskrift, String kommentar, Timestamp frist) {
        return
                new ArbeidslisteDTO(fnr)
                        .setVeilederId(VeilederId.of(SubjectHandler.getIdent().orElseThrow(IllegalStateException::new)))
                        .setOverskrift(overskrift)
                        .setKommentar(kommentar)
                        .setFrist(frist);
    }
}
