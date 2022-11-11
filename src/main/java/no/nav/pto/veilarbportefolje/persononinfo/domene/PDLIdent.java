package no.nav.pto.veilarbportefolje.persononinfo.domene;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
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
}
