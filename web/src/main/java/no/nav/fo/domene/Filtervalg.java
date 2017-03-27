package no.nav.fo.domene;

import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.List;

public class Filtervalg {
    @QueryParam("nyeBrukere")
    public boolean nyeBrukere;

    @QueryParam("inaktiveBrukere")
    public boolean inaktiveBrukere;

    @QueryParam("ytelse")
    public YtelseMapping ytelse;

    @QueryParam("alder[]")
    public List<Integer> alder = new ArrayList<>();

    @QueryParam("kjonn")
    public Integer kjonn;

    @QueryParam("fodselsdagIMnd[]")
    public List<Integer> fodselsdagIMnd = new ArrayList<>();

    @QueryParam("innsatsgruppe[]")
    public List<Integer> innsatsgruppe = new ArrayList<>();

    @QueryParam("formidlingsgruppe[]")
    public List<Integer> formidlingsgruppe = new ArrayList<>();

    public boolean harAktiveFilter() {
        return nyeBrukere ||
                inaktiveBrukere ||
                harYtelsefilter() ||
                (!alder.isEmpty()) ||
                (kjonn != null && (kjonn == 0 || kjonn == 1)) ||
                (!fodselsdagIMnd.isEmpty()) ||
                (!innsatsgruppe.isEmpty()) ||
                (!formidlingsgruppe.isEmpty());
    }

    public boolean harYtelsefilter() {
        return ytelse != null;
    }

    private boolean erMellom(int variabel, int fra, int til) {
        return variabel > fra && variabel <= til;
    }
}
