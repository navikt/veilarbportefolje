package no.nav.pto.veilarbportefolje.persononinfo.PdlResponses;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PDLIdent {
    private String ident;
    private Boolean historisk;
    private Gruppe gruppe;

    public enum Gruppe {
        FOLKEREGISTERIDENT,
        NPID,
        AKTORID
    }
}
