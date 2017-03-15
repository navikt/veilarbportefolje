package no.nav.fo.domene;

import javax.ws.rs.QueryParam;

public class Filtervalg {
    @QueryParam("nyeBrukere")
    public boolean nyeBrukere;

    @QueryParam("inaktiveBrukere")
    public boolean inaktiveBrukere;

    public boolean harAktiveFilter() {
        return nyeBrukere || inaktiveBrukere;
    }
}
