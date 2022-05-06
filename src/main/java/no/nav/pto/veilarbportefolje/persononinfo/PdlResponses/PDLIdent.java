package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.person.pdl.aktor.v2.Type;

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

    // NB: ved endringer må viewt 'aktive_identer' oppdateres
    public enum Gruppe {
        FOLKEREGISTERIDENT,
        NPID,
        AKTORID
    }

    public static Gruppe typeTilGruppe(Type type) {
        return switch (type) {
            case NPID -> Gruppe.NPID;
            case AKTORID -> Gruppe.AKTORID;
            case FOLKEREGISTERIDENT -> Gruppe.FOLKEREGISTERIDENT;
        };
    }
}
