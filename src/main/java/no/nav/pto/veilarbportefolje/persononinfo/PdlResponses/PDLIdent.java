package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.persononinfo.avro.Aktor;

@Data
@Accessors(chain = true)
public class PDLIdent {
    private String ident;
    private boolean historisk;
    private Gruppe gruppe;

    public PDLIdent(String ident, boolean historisk, Gruppe gruppe) {
        this.ident = ident;
        this.historisk = historisk;
        this.gruppe = gruppe;
    }

    public PDLIdent() {
    }

    public enum Gruppe {
        FOLKEREGISTERIDENT,
        NPID,
        AKTORID
    }

    public static Gruppe typeTilGruppe(Aktor.Type type) {
        return switch (type) {
            case NPID -> Gruppe.NPID;
            case AKTORID -> Gruppe.AKTORID;
            case FOLKEREGISTERIDENT -> Gruppe.FOLKEREGISTERIDENT;
        };
    }
}
