package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data()
@Accessors(chain = true)
public class Filtervalg {
    public Brukerstatus brukerstatus;
    public YtelseFilter ytelse;
    public List<String> alder = new ArrayList<>();
    public List<Kjonn> kjonn = new ArrayList<>();
    public List<String> fodselsdagIMnd = new ArrayList<>();
    public List<Innsatsgruppe> innsatsgruppe = new ArrayList<>();
    public List<Formidlingsgruppe> formidlingsgruppe = new ArrayList<>();
    public List<Servicegruppe> servicegruppe = new ArrayList<>();
    public List<Rettighetsgruppe> rettighetsgruppe = new ArrayList<>();
    public List<String> veiledere = new ArrayList<>();

    public boolean harAktiveFilter() {
        return harBrukerstatus() ||
                harYtelsefilter() ||
                !alder.isEmpty() ||
                !kjonn.isEmpty() ||
                !fodselsdagIMnd.isEmpty() ||
                !innsatsgruppe.isEmpty() ||
                !formidlingsgruppe.isEmpty() ||
                !servicegruppe.isEmpty() ||
                !rettighetsgruppe.isEmpty() ||
                !veiledere.isEmpty();
    }

    private boolean harBrukerstatus() {
        return brukerstatus != null;
    }

    public boolean harYtelsefilter() {
        return ytelse != null;
    }
}
