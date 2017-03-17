package no.nav.fo.domene;

import javax.ws.rs.QueryParam;
import java.util.List;

public class Filtervalg {
    @QueryParam("nyeBrukere")
    public boolean nyeBrukere;

    @QueryParam("inaktiveBrukere")
    public boolean inaktiveBrukere;

    @QueryParam("alder[]")
    public List<Integer> alder;

    @QueryParam("kjonn")
    public String kjonn;

    @QueryParam("fodselsdagIMnd")
    public int fodselsdagIMnd;

    public boolean harAktiveFilter() {
        return nyeBrukere || inaktiveBrukere || (alder != null && alder.size() > 0) || ("M".equals(kjonn) || "K".equals(kjonn)) || erMellom(fodselsdagIMnd, 1, 31);
    }

    private boolean erMellom(int variabel, int fra, int til) {
        return variabel > fra && variabel <= til;
    }
}
