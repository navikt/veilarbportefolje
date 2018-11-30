package no.nav.fo.veilarbportefolje.indeksering;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.exception.SolrUpdateResponseCodeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;

import java.text.Collator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.veilarbportefolje.indeksering.SolrSortUtils.addSort;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
public class SolrUtils {
    public static String TILTAK = "TILTAK";

    public static Map<Brukerstatus, String> ferdigfilterStatus = new HashMap<Brukerstatus, String>() {{
        put(Brukerstatus.NYE_BRUKERE, "ny_for_enhet:true");
        put(Brukerstatus.UFORDELTE_BRUKERE, "");
        put(Brukerstatus.TRENGER_VURDERING, "trenger_vurdering:true");
        put(Brukerstatus.INAKTIVE_BRUKERE, "formidlingsgruppekode:ISERV");
        put(Brukerstatus.VENTER_PA_SVAR_FRA_NAV, "venterpasvarfranav:*");
        put(Brukerstatus.VENTER_PA_SVAR_FRA_BRUKER, "venterpasvarfrabruker:*");
        put(Brukerstatus.I_AVTALT_AKTIVITET, "aktiviteter:*");
        put(Brukerstatus.IKKE_I_AVTALT_AKTIVITET, "-aktiviteter:*");
        put(Brukerstatus.UTLOPTE_AKTIVITETER, "nyesteutlopteaktivitet:*");
        put(Brukerstatus.MIN_ARBEIDSLISTE, "arbeidsliste_aktiv:*");
        put(Brukerstatus.NYE_BRUKERE_FOR_VEILEDER, "ny_for_veileder:true");
        put(Brukerstatus.ER_SYKMELDT_MED_ARBEIDSGIVER, formidlingsgruppekodeOgKvalifiseringsgruppeKoderErSykmeldtMedArbeidsgiver());
    }};

    private static Locale locale = new Locale("no", "NO");
    private static Collator collator = Collator.getInstance(locale);

    static {
        collator.setStrength(Collator.PRIMARY);
    }

    public static FacetResults mapFacetResults(FacetField facetField) {
        return new FacetResults()
                .setFacetResults(
                        facetField.getValues().stream().map(
                                value -> new Facet()
                                        .setValue(value.getName())
                                        .setCount(value.getCount())
                        ).collect(toList())
                );
    }

    public static SolrQuery buildSolrFacetQuery(String query, String facetField) {
        SolrQuery solrQuery = new SolrQuery(query);
        solrQuery.setFacet(true);
        solrQuery.addFacetField(facetField);
        solrQuery.setFacetLimit(-1);
        return solrQuery;
    }

    public static String byggVeilederFilterQueryString(String enhetId, Optional<String> veilederIdent) {
        return veilederIdent
                .map((ident) -> isBlank(ident) ? null : ident)
                .map((ident) -> "veileder_id: " + ident + " AND enhet_id: " + enhetId)
                .orElse("enhet_id: " + enhetId);
    }

    public static SolrQuery byggHovedSolrQuery (Optional<String> veilederIdent, Filtervalg filtervalg) {
        SolrQuery solrQuery = new SolrQuery("*:*");

        veilederIdent.ifPresent(veilderId -> {
            if(isNotBlank(veilderId) && filtervalg.harNavnEllerFnrQuery()){
                Try.of(()-> Integer.parseInt(filtervalg.navnEllerFnrQuery))
                        .onSuccess((fnr) -> solrQuery.setQuery("fnr:" + filtervalg.navnEllerFnrQuery + "*"))
                        .onFailure((stringValue) -> solrQuery.setQuery("navn_sok: " + filtervalg.navnEllerFnrQuery));
            }
        });

        return solrQuery;
    }

    public static SolrQuery buildSolrQuery(String enhetId, Optional<String> veilederIdent, List<VeilederId> veiledereMedTilgang, String sortOrder, String sortField, Filtervalg filtervalg) {
        boolean sorterNyeForVeileder = veilederIdent.map(StringUtils::isNotBlank).orElse(false);

        SolrQuery solrQuery = byggHovedSolrQuery(veilederIdent, filtervalg);

        solrQuery.addField("*");

        Optional<String> medTilgangSubquery = harVeilederSubQuery(veiledereMedTilgang);

        medTilgangSubquery.ifPresent(subquery -> solrQuery.addField("har_veileder_fra_enhet:" + subquery));
        solrQuery.addFilterQuery(byggVeilederFilterQueryString(enhetId, veilederIdent));
        addSort(solrQuery, sorterNyeForVeileder, medTilgangSubquery, sortOrder, sortField, filtervalg);
        leggTilFiltervalg(solrQuery, filtervalg, veiledereMedTilgang);
        return solrQuery;
    }

    public static void checkSolrResponseCode(int statusCode) {
        if (statusCode != 0) {
            throw new SolrUpdateResponseCodeException(format("Solr returnerte med statuskode %s", statusCode));
        }
    }

    public static <T> String orStatement(List<T> filter, Function<T, String> mapper) {
        if (filter == null || filter.isEmpty()) {
            return "";
        }
        return filter.stream().map(mapper).collect(joining(" OR "));
    }


    public static String formidlingsgruppekodeOgKvalifiseringsgruppeKoderErSykmeldtMedArbeidsgiver() {
        return "formidlingsgruppekode:IARBS AND !(" + asList("BATT", "BFORM", "IKVAL", "VURDU", "OPPFI", "VARIG")
                .stream()
                .map(kvalifiseringsgruppeKode -> "kvalifiseringsgruppekode:" + kvalifiseringsgruppeKode)
                .collect(Collectors.joining(" + ")) + ")";
    }

    private static Optional<String> getFiltrerBrukerStatement(Filtervalg filtervalg) {
        final List<String> filtrerBrukereStatements = new ArrayList<>();
        filtrerBrukereStatements.add(orStatement(filtervalg.alder, SolrUtils::alderFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.kjonn, SolrUtils::kjonnFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.fodselsdagIMnd, SolrUtils::fodselsdagIMndFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.innsatsgruppe, SolrUtils::innsatsgruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.hovedmal, SolrUtils::hovedmalFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.formidlingsgruppe, SolrUtils::formidlingsgruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.servicegruppe, SolrUtils::servicegruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.rettighetsgruppe, SolrUtils::rettighetsgruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.veiledere, SolrUtils::veilederFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.manuellBrukerStatus, SolrUtils::manuellStatusFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.tiltakstyper, SolrUtils::tiltakJaFilter));
        Optional.ofNullable(filtervalg.ytelse).ifPresent(ytelse -> filtrerBrukereStatements.add(orStatement(ytelse.underytelser, SolrUtils::ytelseFilter)));

        if (filtervalg.harAktivitetFilter()) {
            filtervalg.aktiviteter.forEach((key, value) -> {
                if (key.equals(TILTAK)) {
                    leggTilTiltakJaNeiFilter(filtrerBrukereStatements, value);
                } else {
                    leggTilAktivitetFiltervalg(filtrerBrukereStatements, key, value);
                }
            });
        }

        String filter = filtrerBrukereStatements.stream()
                .filter(StringUtils::isNotBlank)
                .map(statement -> "(" + statement + ")")
                .collect(Collectors.joining(" AND "));

        if (StringUtils.isNotBlank(filter)) {
            return Optional.of(filter);
        }
        return Optional.empty();
    }

    private static Optional<String> getFerdigFilterStatement(Filtervalg filtervalg, List<VeilederId> veiledereMedTilgang) {
        List<String> ferdigFilterStatements = ferdigFilterListeEllerBrukerstatus(filtervalg)
                .stream()
                .map(brukerstatus -> getFerdigFilterStatus(brukerstatus, veiledereMedTilgang))
                .filter(StringUtils::isNotBlank)
                .collect(toList());

        if (!ferdigFilterStatements.isEmpty()) {
            return Optional.of("(" + StringUtils.join(ferdigFilterStatements, " AND ") + ")");
        }
        return Optional.empty();
    }

    private static String getFerdigFilterStatus(Brukerstatus brukerstatus, List<VeilederId> veiledereMedTilgang) {
        if (Brukerstatus.UFORDELTE_BRUKERE == brukerstatus) {
            return harIkkeVeilederFilter(veiledereMedTilgang).orElse("");
        }
        return ferdigfilterStatus.get(brukerstatus);
    }

    private static void leggTilFiltervalg(SolrQuery query, Filtervalg filtervalg, List<VeilederId> veiledereMedTilgang) {
        if (!filtervalg.harAktiveFilter()) {
            return;
        }

        getFerdigFilterStatement(filtervalg, veiledereMedTilgang).ifPresent(query::addFilterQuery);
        getFiltrerBrukerStatement(filtervalg).ifPresent(query::addFilterQuery);
    }

    public static Optional<String> harIkkeVeilederFilter(List<VeilederId> identer) {
        if (identer.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("-veileder_id:(" + spaceSeperated(identer) + ")");
    }

    private static String spaceSeperated(List<VeilederId> veilederListe) {
        return veilederListe
                .stream()
                .map(VeilederId::getVeilederId)
                .collect(Collectors.joining(" "));
    }

    private static List<Brukerstatus> ferdigFilterListeEllerBrukerstatus(Filtervalg filtervalg) {
        List<Brukerstatus> ferdigfilterListe = filtervalg.ferdigfilterListe;
        if (ferdigfilterListe != null && !ferdigfilterListe.isEmpty()) {
            return ferdigfilterListe;
        }
        return Collections.emptyList();
    }

    public static void leggTilAktivitetFiltervalg(List<String> filtrerBrukereStatements, String key, AktivitetFiltervalg value) {
        if (AktivitetFiltervalg.JA.equals(value)) {
            filtrerBrukereStatements.add("aktiviteter:" + key.toLowerCase());
        }
        if (AktivitetFiltervalg.NEI.equals(value)) {
            filtrerBrukereStatements.add("*:* AND -aktiviteter:" + key.toLowerCase());
        }
    }

    public static void leggTilTiltakJaNeiFilter(List<String> filtrerBrukereStatements, AktivitetFiltervalg value) {
        if (AktivitetFiltervalg.JA.equals(value)) {
            filtrerBrukereStatements.add("tiltak:*");
        }
        if (AktivitetFiltervalg.NEI.equals(value)) {
            filtrerBrukereStatements.add("*:* AND -tiltak:*");
        }
    }

    private static String ytelseFilter(YtelseMapping ytelse) {
        return "ytelse:" + ytelse;
    }

    public static String kjonnFilter(Kjonn kjonn) {
        return "kjonn:" + kjonn.toString();
    }

    public static String alderFilter(String alder) {
        return "fodselsdato:" + FiltervalgMappers.alder.get(alder);
    }

    public static String fodselsdagIMndFilter(String fodselDato) {
        return "fodselsdag_i_mnd:" + fodselDato;
    }

    public static String innsatsgruppeFilter(Innsatsgruppe innsatsgruppe) {
        return "kvalifiseringsgruppekode:" + innsatsgruppe;
    }

    public static String formidlingsgruppeFilter(Formidlingsgruppe formidlingsgruppe) {
        return "formidlingsgruppekode:" + formidlingsgruppe;
    }

    public static String servicegruppeFilter(Servicegruppe servicegruppe) {
        return "kvalifiseringsgruppekode:" + servicegruppe;
    }

    public static String rettighetsgruppeFilter(Rettighetsgruppe rettighetsgruppe) {
        return "rettighetsgruppekode:" + rettighetsgruppe;
    }

    public static String veilederFilter(String veileder) {
        return "veileder_id:" + veileder;
    }

    public static String tiltakJaFilter(String tiltak) {
        return "tiltak:" + tiltak;
    }

    public static Optional<String> harVeilederSubQuery(List<VeilederId> identer) {
        if (identer.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("exists(query({!v='veileder_id:(" + spaceSeperated(identer) + " )'}))");
    }

    public static String manuellStatusFilter(ManuellBrukerStatus manuellBrukerStatus) {
        return "manuell_bruker:" + manuellBrukerStatus.toString();
    }

    public static String hovedmalFilter(Hovedmal hovedmal) {
        return "hovedmaalkode:" + hovedmal;
    }
}
