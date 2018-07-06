package no.nav.fo.util;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.domene.*;
import no.nav.fo.exception.SolrUpdateResponseCodeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;

import java.text.Collator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.SolrSortUtils.addSort;

@Slf4j
public class SolrUtils {
    static String TILTAK = "TILTAK";

    static Map<Brukerstatus, String> ferdigfilterStatus = new HashMap<Brukerstatus, String>() {{
        put(Brukerstatus.NYE_BRUKERE, "ny_for_enhet:true");
        put(Brukerstatus.UFORDELTE_BRUKERE, "ny_for_enhet:true");
        put(Brukerstatus.INAKTIVE_BRUKERE, "formidlingsgruppekode:ISERV");
        put(Brukerstatus.VENTER_PA_SVAR_FRA_NAV, "venterpasvarfranav:*");
        put(Brukerstatus.VENTER_PA_SVAR_FRA_BRUKER, "venterpasvarfrabruker:*");
        put(Brukerstatus.I_AVTALT_AKTIVITET, "aktiviteter:*");
        put(Brukerstatus.IKKE_I_AVTALT_AKTIVITET, "-aktiviteter:*");
        put(Brukerstatus.UTLOPTE_AKTIVITETER, "nyesteutlopteaktivitet:*");
        put(Brukerstatus.MIN_ARBEIDSLISTE, "arbeidsliste_aktiv:*");
        put(Brukerstatus.NYE_BRUKERE_FOR_VEILEDER, "ny_for_veileder:true");
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


    public static SolrQuery buildSolrQuery(String queryString, boolean sorterNyeForVeileder, List<VeilederId> veiledereMedTilgang, String sortOrder, String sortField, Filtervalg filtervalg) {
        SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.addField("*");

        String medTilgangSubquery = null;
        if(veiledereMedTilgang != null) { //FO-610 rydde
            medTilgangSubquery = harVeilederSubQuery(veiledereMedTilgang);
            solrQuery.addField("har_veileder_fra_enhet:" + medTilgangSubquery);
        }
        solrQuery.addFilterQuery(queryString);
        addSort(solrQuery, sorterNyeForVeileder, medTilgangSubquery, sortOrder, sortField, filtervalg);
        leggTilFiltervalg(solrQuery, filtervalg, veiledereMedTilgang);
        return solrQuery;
    }

    public static boolean isSlaveNode() {
        String isMasterString = System.getProperty("cluster.ismasternode", "false");
        return !Boolean.parseBoolean(isMasterString);
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

    private static void leggTilFiltervalg(SolrQuery query, Filtervalg filtervalg, List<VeilederId> veiledereMedTilgang) {
        if (!filtervalg.harAktiveFilter()) {
            return;
        }

        final List<String> filtrerBrukereStatements = new ArrayList<>();

        List<Brukerstatus> brukerstatuses = ferdigFilterListeEllerBrukerstatus(filtervalg);

        Optional<String> ufordelteBrukere = Optional.empty();

        if(veiledereMedTilgang != null && brukerstatuses.contains(Brukerstatus.UFORDELTE_BRUKERE)) {//FO-610 rydde
            ferdigfilterStatus.remove(Brukerstatus.UFORDELTE_BRUKERE);
            ufordelteBrukere = Optional.of(harIkkeVeilederFilter(veiledereMedTilgang));
        }

        List<String> ferdigFilterStatements = brukerstatuses
                .stream()
                .map(ferdigfilter -> ferdigfilterStatus.get(ferdigfilter))
                .filter(Objects::nonNull) //FO-610 rydde
                .collect(toList());

        ufordelteBrukere.ifPresent(ferdigFilterStatements::add);//FO-610 rydde

        filtrerBrukereStatements.add(orStatement(filtervalg.alder, SolrUtils::alderFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.kjonn, SolrUtils::kjonnFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.fodselsdagIMnd, SolrUtils::fodselsdagIMndFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.innsatsgruppe, SolrUtils::innsatsgruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.formidlingsgruppe, SolrUtils::formidlingsgruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.servicegruppe, SolrUtils::servicegruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.rettighetsgruppe, SolrUtils::rettighetsgruppeFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.veiledere, SolrUtils::veilederFilter));
        filtrerBrukereStatements.add(orStatement(filtervalg.manuellBrukerStatus, SolrUtils::manuellStatusFilter));

        if (filtervalg.harAktivitetFilter()) {
            filtervalg.aktiviteter.forEach((key, value) -> {
                if (key.equals(TILTAK)) {
                    leggTilTiltakJaNeiFilter(filtrerBrukereStatements, value);
                } else {
                    leggTilAktivitetFiltervalg(filtrerBrukereStatements, key, value);
                }
            });
        }

        if (filtervalg.harTiltakstypeFilter()) {
            filtrerBrukereStatements.add(orStatement(filtervalg.tiltakstyper, SolrUtils::tiltakJaFilter));
        }

        if (filtervalg.harYtelsefilter()) {
            filtrerBrukereStatements.add(orStatement(filtervalg.ytelse.underytelser, SolrUtils::ytelseFilter));
        }

        if (!ferdigFilterStatements.isEmpty()) {
            query.addFilterQuery("(" + StringUtils.join(ferdigFilterStatements, " AND ") + ")");
        }

        if (!filtrerBrukereStatements.isEmpty()) {
            query.addFilterQuery(filtrerBrukereStatements
                    .stream()
                    .filter(StringUtils::isNotBlank)
                    .map(statement -> "(" + statement + ")")
                    .collect(Collectors.joining(" AND ")));
        }
    }

    public static String harIkkeVeilederFilter(List<VeilederId> identer) {
        return "-veileder_id:(" + spaceSeperated(identer) + ")";
    }

    private static String spaceSeperated(List<VeilederId> veilederListe) {
        return veilederListe
                .stream()
                .map(VeilederId::getVeilederId)
                .collect(Collectors.joining(" "));
    }

    private static List<Brukerstatus> ferdigFilterListeEllerBrukerstatus(Filtervalg filtervalg) {
        List<Brukerstatus> ferdigfilterListe = filtervalg.ferdigfilterListe;
        Brukerstatus brukerstatus = filtervalg.brukerstatus;
        if (ferdigfilterListe != null && !ferdigfilterListe.isEmpty()) {
            return ferdigfilterListe;
        } else if (brukerstatus != null) {
            return Collections.singletonList(brukerstatus);
        } else {
            return Collections.emptyList();
        }
    }

    static void leggTilAktivitetFiltervalg(List<String> filtrerBrukereStatements, String key, AktivitetFiltervalg value) {
        if (AktivitetFiltervalg.JA.equals(value)) {
            filtrerBrukereStatements.add("aktiviteter:" + key.toLowerCase());
        }
        if (AktivitetFiltervalg.NEI.equals(value)) {
            filtrerBrukereStatements.add("*:* AND -aktiviteter:" + key.toLowerCase());
        }
    }

    static void leggTilTiltakJaNeiFilter(List<String> filtrerBrukereStatements, AktivitetFiltervalg value) {
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

    static String kjonnFilter(Kjonn kjonn) {
        return "kjonn:" + kjonn.toString();
    }

    static String alderFilter(String alder) {
        return "fodselsdato:" + FiltervalgMappers.alder.get(alder);
    }

    static String fodselsdagIMndFilter(String fodselDato) {
        return "fodselsdag_i_mnd:" + fodselDato;
    }

    static String innsatsgruppeFilter(Innsatsgruppe innsatsgruppe) {
        return "kvalifiseringsgruppekode:" + innsatsgruppe;
    }

    static String formidlingsgruppeFilter(Formidlingsgruppe formidlingsgruppe) {
        return "formidlingsgruppekode:" + formidlingsgruppe;
    }

    static String servicegruppeFilter(Servicegruppe servicegruppe) {
        return "kvalifiseringsgruppekode:" + servicegruppe;
    }

    static String rettighetsgruppeFilter(Rettighetsgruppe rettighetsgruppe) {
        return "rettighetsgruppekode:" + rettighetsgruppe;
    }

    static String veilederFilter(String veileder) {
        return "veileder_id:" + veileder;
    }

    static String tiltakJaFilter(String tiltak) {
        return "tiltak:" + tiltak;
    }

    static String harVeilederSubQuery(List<VeilederId> identer) {
        return "exists(query({!v='veileder_id:(" + spaceSeperated(identer) + " )'}))";
    }

    static String manuellStatusFilter(ManuellBrukerStatus manuellBrukerStatus) {
        return "manuell_bruker:" + manuellBrukerStatus.toString();
    }
}
