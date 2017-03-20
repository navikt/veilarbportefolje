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

    @QueryParam("fodselsdagIMnd[]")
    public List<Integer> fodselsdagIMnd;

    public boolean harAktiveFilter() {
        return nyeBrukere || inaktiveBrukere || (alder != null && alder.size() > 0) || ("M".equals(kjonn) || "K".equals(kjonn)) || (fodselsdagIMnd != null && fodselsdagIMnd.size() > 0);
    }

    private boolean erMellom(int variabel, int fra, int til) {
        return variabel > fra && variabel <= til;
    }
}
