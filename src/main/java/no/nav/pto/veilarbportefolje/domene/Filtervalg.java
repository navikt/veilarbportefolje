package no.nav.pto.veilarbportefolje.domene;

import lombok.Data;
import lombok.experimental.Accessors;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.JobbSituasjonBeskrivelse;
import no.nav.pto.veilarbportefolje.domene.filtervalg.UtdanningBestattSvar;
import no.nav.pto.veilarbportefolje.domene.filtervalg.UtdanningGodkjentSvar;
import no.nav.pto.veilarbportefolje.domene.filtervalg.UtdanningSvar;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.avvik14aVedtak.Avvik14aVedtak;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringsKategori;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.BinaryOperator;

import static java.lang.Integer.parseInt;

@Data()
@Accessors(chain = true)
public class Filtervalg {
    public Brukerstatus brukerstatus; // finst ikkje i veilarbportefoljeflatefs
    public List<Brukerstatus> ferdigfilterListe;
    public YtelseFilter ytelse;
    public List<String> alder = new ArrayList<>();
    public Kjonn kjonn;
    public List<String> fodselsdagIMnd = new ArrayList<>();
    public List<ArenaInnsatsgruppe> innsatsgruppe = new ArrayList<>();
    public List<ArenaHovedmal> hovedmal = new ArrayList<>();
    public List<Formidlingsgruppe> formidlingsgruppe = new ArrayList<>();
    public List<Servicegruppe> servicegruppe = new ArrayList<>();
    public List<Rettighetsgruppe> rettighetsgruppe = new ArrayList<>();
    public List<String> veiledere = new ArrayList<>();
    public Map<String, AktivitetFiltervalg> aktiviteter = new HashMap<>();
    public List<String> tiltakstyper = new ArrayList<>();
    public List<ManuellBrukerStatus> manuellBrukerStatus = new ArrayList<>();
    public String navnEllerFnrQuery;
    public List<JobbSituasjonBeskrivelse> registreringstype = new ArrayList<>();
    public List<UtdanningSvar> utdanning = new ArrayList<>();
    public List<UtdanningBestattSvar> utdanningBestatt = new ArrayList<>();
    public List<UtdanningGodkjentSvar> utdanningGodkjent = new ArrayList<>();
    public List<String> sisteEndringKategori = new ArrayList<>();
    public List<String> aktiviteterForenklet = new ArrayList<>();
    public List<String> alleAktiviteter = new ArrayList<>(); // finst ikkje i veilarbportefoljeflatefs
    public String ulesteEndringer;
    public CVjobbprofil cvJobbprofil;
    public List<String> landgruppe;
    public List<String> foedeland;
    public List<String> tolkebehov;
    public List<String> tolkBehovSpraak;
    public List<StillingFraNAVFilter> stillingFraNavFilter;
    public List<BarnUnder18Aar> barnUnder18Aar = new ArrayList<>();
    public List<String> barnUnder18AarAlder = new ArrayList<>();
    public List<String> geografiskBosted;
    public List<Avvik14aVedtak> avvik14aVedtak;
    public List<EnsligeForsorgere> ensligeForsorgere;
    public List<String> fargekategorier = new ArrayList<>();
    public List<String> gjeldendeVedtak14a = new ArrayList<>();
    public List<Innsatsgruppe> innsatsgruppeGjeldendeVedtak14a = new ArrayList<>();
    public List<Hovedmal> hovedmalGjeldendeVedtak14a = new ArrayList<>();

    public boolean harAktiveFilter() {
        return harFerdigFilter() ||
                harYtelsefilter() ||
                !alder.isEmpty() ||
                harKjonnfilter() ||
                !fodselsdagIMnd.isEmpty() ||
                !innsatsgruppe.isEmpty() ||
                !formidlingsgruppe.isEmpty() ||
                !servicegruppe.isEmpty() ||
                !rettighetsgruppe.isEmpty() ||
                !veiledere.isEmpty() ||
                !aktiviteter.isEmpty() ||
                !tiltakstyper.isEmpty() ||
                !hovedmal.isEmpty() ||
                !registreringstype.isEmpty() ||
                !utdanning.isEmpty() ||
                !utdanningBestatt.isEmpty() ||
                !utdanningGodkjent.isEmpty() ||
                !sisteEndringKategori.isEmpty() ||
                harAktiviteterForenklet() ||
                harCvFilter() ||
                harManuellBrukerStatus() ||
                harNavnEllerFnrQuery() ||
                harTolkbehovSpraakFilter() ||
                harTegnspraakFilter() ||
                harTalespraaktolkFilter() ||
                harFoedelandFilter() ||
                harStillingFraNavFilter() ||
                harBarnUnder18AarFilter() ||
                harLandgruppeFilter() ||
                harBostedFilter() ||
                harAvvik14aVedtakFilter() ||
                harEnsligeForsorgereFilter() ||
                harFargeKategoriFilter() ||
                harGjeldendeVedtak14aFilter() ||
                harInnsatsgruppeGjeldendeVedtak14a() ||
                harHovedmalGjeldendeVedtak14a();
    }

    public boolean harGjeldendeVedtak14aFilter() {
        return gjeldendeVedtak14a != null && !gjeldendeVedtak14a.isEmpty();
    }

    public boolean harInnsatsgruppeGjeldendeVedtak14a() {
        return innsatsgruppeGjeldendeVedtak14a != null && !innsatsgruppeGjeldendeVedtak14a.isEmpty();
    }

    public boolean harHovedmalGjeldendeVedtak14a() {
        return hovedmalGjeldendeVedtak14a != null && !hovedmalGjeldendeVedtak14a.isEmpty();
    }

    public boolean harEnsligeForsorgereFilter() {
        return ensligeForsorgere != null && !ensligeForsorgere.isEmpty();
    }

    public boolean harCvFilter() {
        return cvJobbprofil != null;
    }

    public boolean harFerdigFilter() {
        return brukerstatus != null || (ferdigfilterListe != null && !ferdigfilterListe.isEmpty());
    }

    public boolean harYtelsefilter() {
        return ytelse != null;
    }

    public boolean harKjonnfilter() {
        return kjonn != null;
    }

    public boolean harAktivitetFilter() {
        return tiltakstyper != null;
    }

    public boolean harSisteEndringFilter() {
        return !sisteEndringKategori.isEmpty();
    }

    public boolean harManuellBrukerStatus() {
        return manuellBrukerStatus != null && !manuellBrukerStatus.isEmpty();
    }

    public boolean harAktiviteterForenklet() {
        return !aktiviteterForenklet.isEmpty();
    }

    public boolean harNavnEllerFnrQuery() {
        return StringUtils.isNotBlank(navnEllerFnrQuery);
    }

    public boolean harUlesteEndringerFilter() {
        return StringUtils.isNotBlank(ulesteEndringer);
    }

    public boolean harFoedelandFilter() {
        return foedeland != null && !foedeland.isEmpty();
    }

    public boolean harTolkbehovSpraakFilter() {
        return tolkBehovSpraak != null && !tolkBehovSpraak.isEmpty();
    }

    public boolean harTalespraaktolkFilter() {
        return tolkebehov != null && tolkebehov.contains("TALESPRAAKTOLK");
    }

    public boolean harTegnspraakFilter() {
        return tolkebehov != null && tolkebehov.contains("TEGNSPRAAKTOLK");
    }

    public boolean harLandgruppeFilter() {
        return landgruppe != null && !landgruppe.isEmpty();
    }

    public boolean harFargeKategoriFilter() {
        return fargekategorier != null && !fargekategorier.isEmpty();
    }

    public boolean harStillingFraNavFilter() {
        return stillingFraNavFilter != null && !stillingFraNavFilter.isEmpty();
    }

    public boolean harBarnUnder18AarFilter() {
        return (barnUnder18Aar != null && !barnUnder18Aar.isEmpty()) || (barnUnder18AarAlder != null && !barnUnder18AarAlder.isEmpty());
    }

    public boolean harBostedFilter() {
        return geografiskBosted != null && !geografiskBosted.isEmpty();
    }

    public boolean harAvvik14aVedtakFilter() {
        return avvik14aVedtak != null && !avvik14aVedtak.isEmpty();
    }

    public boolean harDinSituasjonSvar() {
        return registreringstype != null && !registreringstype.isEmpty();
    }

    public boolean harUtdanningSvar() {
        return utdanning != null && !utdanning.isEmpty();
    }

    public boolean harUtdanningBestattSvar() {
        return utdanningBestatt != null && !utdanningBestatt.isEmpty();
    }

    public boolean harUtdanningGodkjentSvar() {
        return utdanningGodkjent != null && !utdanningGodkjent.isEmpty();
    }

    public boolean valider() {
        if (!harAktiveFilter()) {
            return true;
        }

        Boolean alderOk = alder
                .stream()
                .map(Filtervalg::erGyldigAldersSpenn)
                .reduce(true, and());

        Boolean fodselsdatoOk = fodselsdagIMnd
                .stream()
                .map((dato) -> dato.matches("\\d+"))
                .reduce(true, and());

        Boolean veiledereOk = veiledere
                .stream()
                .map((veileder) -> veileder.matches("[A-Z]\\d{6}"))
                .reduce(true, and());

        Boolean utdanningOK = utdanning
                .stream()
                .map(Objects::nonNull)
                .reduce(true, and());

        Boolean sisteEndringOK = sisteEndringKategori
                .stream()
                .map(SisteEndringsKategori::contains)
                .reduce(true, and());

        Boolean barnAlderOk = barnUnder18AarAlder
                .stream()
                .map(Filtervalg::erGyldigAldersSpenn)
                .reduce(true, and());

        Boolean gjeldendeVedtak14aOk = gjeldendeVedtak14a
                .stream()
                .map(GjeldendeVedtak14aFilter::contains)
                .reduce(true, and());

        Boolean innsatsgruppeGjeldendeVedtak14aOk = innsatsgruppeGjeldendeVedtak14a
                .stream()
                .map(Innsatsgruppe::contains)
                .reduce(true, and());

        Boolean hovedmalGjeldendeVedtak14aOk = hovedmalGjeldendeVedtak14a
                .stream()
                .map(Hovedmal::contains)
                .reduce(true, and());

        return alderOk && fodselsdatoOk && veiledereOk && utdanningOK && sisteEndringOK && barnAlderOk &&
                gjeldendeVedtak14aOk && innsatsgruppeGjeldendeVedtak14aOk && hovedmalGjeldendeVedtak14aOk;
    }

    private BinaryOperator<Boolean> and() {
        return (aBoolean, aBoolean2) -> aBoolean && aBoolean2;
    }

    public static boolean erGyldigAldersSpenn(String fraTilAlderIput) {
        String[] fraTilAlder = fraTilAlderIput.split("-");
        return fraTilAlder.length == 2 && parseInt(fraTilAlder[0]) <= parseInt(fraTilAlder[1]);
    }


}
