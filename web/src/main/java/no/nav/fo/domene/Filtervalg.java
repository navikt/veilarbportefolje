package no.nav.fo.domene;

import java.util.ArrayList;
import java.util.List;

public class Filtervalg {
    public boolean nyeBrukere;
    public boolean inaktiveBrukere;
    public YtelseMapping ytelse;
    public List<String> alder = new ArrayList<>();
    public List<Kjonn> kjonn = new ArrayList<>();
    public List<String> fodselsdagIMnd = new ArrayList<>();
    public List<Innsatsgruppe> innsatsgruppe = new ArrayList<>();
    public List<Formidlingsgruppe> formidlingsgruppe = new ArrayList<>();
    public List<Servicegruppe> servicegruppe = new ArrayList<>();

    public boolean harAktiveFilter() {
        return nyeBrukere ||
                inaktiveBrukere ||
                harYtelsefilter() ||
                !alder.isEmpty() ||
                !kjonn.isEmpty() ||
                !fodselsdagIMnd.isEmpty() ||
                !innsatsgruppe.isEmpty() ||
                !formidlingsgruppe.isEmpty() ||
                !servicegruppe.isEmpty();
    }

    public boolean harYtelsefilter() {
        return ytelse != null;
    }
}
