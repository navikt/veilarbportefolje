package no.nav.pto.veilarbportefolje.arbeidsliste;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.domene.value.Fnr;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;

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
    Arbeidsliste.Kategori kategori;
    String navKontorForArbeidsliste;

    public static ArbeidslisteDTO of(Fnr fnr, String overskrift, String kommentar, Timestamp frist, Arbeidsliste.Kategori kategori) {
        return
                new ArbeidslisteDTO(fnr)
                        .setVeilederId(VeilederId.of(SubjectHandler.getIdent().orElseThrow(IllegalStateException::new)))
                        .setOverskrift(overskrift)
                        .setKommentar(kommentar)
                        .setKategori(kategori)
                        .setFrist(frist);
    }
}
