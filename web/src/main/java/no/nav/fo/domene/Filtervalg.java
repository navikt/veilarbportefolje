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
    public Integer kjonn;

    @QueryParam("fodselsdagIMnd[]")
    public List<Integer> fodselsdagIMnd;

    @QueryParam("innsatsgruppe[]")
    public List<Integer> innsatsgruppe;

    @QueryParam("formidlingsgruppe[]")
    public List<Integer> formidlingsgruppe;

    public boolean harAktiveFilter() {
        return nyeBrukere ||
                inaktiveBrukere ||
                (alder != null && alder.size() > 0) ||
                (kjonn != null && (kjonn == 0  || kjonn == 1)) ||
                (fodselsdagIMnd != null && fodselsdagIMnd.size() > 0) ||
                (innsatsgruppe != null && innsatsgruppe.size() > 0) ||
                (formidlingsgruppe != null && formidlingsgruppe.size() > 0);
    }
}
