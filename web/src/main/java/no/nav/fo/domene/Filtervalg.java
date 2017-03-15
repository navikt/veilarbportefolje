package no.nav.fo.domene;

import javax.ws.rs.QueryParam;

public class Filtervalg {
    @QueryParam("nyeBrukere")
    public boolean nyeBrukere;

    @QueryParam("inaktiveBrukere")
    public boolean inaktiveBrukere;

    @QueryParam("alder")
    public int alder;

    @QueryParam("kjonn")
    public String kjonn;

    @QueryParam("fodselsdagIMnd")
    public int fodselsdagIMnd;

    public boolean harAktiveFilter() {
        return nyeBrukere || inaktiveBrukere || (alder > 0 && alder <= 8)|| ("M".equals(kjonn) || "K".equals(kjonn)) || (fodselsdagIMnd > 0 && fodselsdagIMnd <= 31);
    }
}
