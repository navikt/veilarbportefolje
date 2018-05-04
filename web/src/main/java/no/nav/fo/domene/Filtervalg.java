package no.nav.fo.domene;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

import static java.util.Arrays.asList;

@Data()
@Accessors(chain = true)
public class Filtervalg {
    public Brukerstatus brukerstatus;
    public List<Brukerstatus> ferdigfilterListe;
    public YtelseFilter ytelse;
    public List<String> alder = new ArrayList<>();
    public List<Kjonn> kjonn = new ArrayList<>();
    public List<String> fodselsdagIMnd = new ArrayList<>();
    public List<Innsatsgruppe> innsatsgruppe = new ArrayList<>();
    public List<Formidlingsgruppe> formidlingsgruppe = new ArrayList<>();
    public List<Servicegruppe> servicegruppe = new ArrayList<>();
    public List<Rettighetsgruppe> rettighetsgruppe = new ArrayList<>();
    public List<String> veiledere = new ArrayList<>();
    public Map<String, AktivitetFiltervalg> aktiviteter = new HashMap<>();
    public List<String> tiltakstyper = new ArrayList<>();
    public List<ManuellBrukere> manuellbrukere = new ArrayList<>();   //TODO Slett når FO-123 er i prod
    public List<ManuellBrukerStatus> manuellBrukerStatus = new ArrayList<>();

    public boolean harAktiveFilter() {
         return harFerdigFilter() ||
                harYtelsefilter() ||
                !alder.isEmpty() ||
                !kjonn.isEmpty() ||
                !fodselsdagIMnd.isEmpty() ||
                !innsatsgruppe.isEmpty() ||
                !formidlingsgruppe.isEmpty() ||
                !servicegruppe.isEmpty() ||
                !rettighetsgruppe.isEmpty() ||
                !veiledere.isEmpty() ||
                !aktiviteter.isEmpty() ||
                !tiltakstyper.isEmpty() ||
                 harManuellbrukere() ||   //TODO Slett når FO-123 er i prod
                 harManuellBrukerStatus();
    }

    private boolean harFerdigFilter() {
        return brukerstatus != null || (ferdigfilterListe != null && !ferdigfilterListe.isEmpty());
    }

    public boolean harYtelsefilter() {
        return ytelse != null;
    }

    public boolean harAktivitetFilter() {
        return tiltakstyper != null;
    }

    public boolean harTiltakstypeFilter() {
        return !tiltakstyper.isEmpty();
    }

    //TODO Slett når FO-123 er i prod
    public boolean harManuellbrukere() {
        return manuellbrukere != null && !manuellbrukere.isEmpty();
    }

    public boolean harManuellBrukerStatus() {
        return manuellBrukerStatus != null && !manuellBrukerStatus.isEmpty();
    }

    public boolean valider() {
        if (!harAktiveFilter()) {
            return true;
        }

        Boolean alderOk = alder
                .stream()
                .map(FiltervalgMappers.alder::containsKey)
                .reduce(true, and());

        Boolean fodselsdatoOk = fodselsdagIMnd
                .stream()
                .map((dato) -> dato.matches("\\d+"))
                .reduce(true, and());
        Boolean veiledereOk = veiledere
                .stream()
                .map((veileder) -> veileder.matches("[A-Z]\\d{6}"))
                .reduce(true, and());

        return alderOk && fodselsdatoOk && veiledereOk;
    }

    private BinaryOperator<Boolean> and() {
        return (aBoolean, aBoolean2) -> aBoolean && aBoolean2;
    }
}
