package no.nav.pto.veilarbportefolje.arbeidsliste;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.common.auth.context.AuthContextHolderThreadLocal;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.Id;
import no.nav.pto.veilarbportefolje.domene.value.VeilederId;

import java.sql.Timestamp;

@Data
@Accessors(chain = true)
public class ArbeidslisteDTO {
    final Fnr fnr;
    AktorId AktorId;
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
                        .setVeilederId(VeilederId.of(AuthContextHolderThreadLocal
                                .instance().getNavIdent().map(Id::toString).orElseThrow(IllegalStateException::new)))
                        .setOverskrift(overskrift)
                        .setKommentar(kommentar)
                        .setKategori(kategori)
                        .setFrist(frist);
    }
}
