package no.nav.fo.util;

import no.nav.fo.domene.*;
import no.nav.fo.exception.SolrUpdateResponseCodeException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static no.nav.fo.util.SolrUtils.TILTAK;
import static no.nav.fo.util.SolrUtils.orStatement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SolrUtilsTest {

    static String[] alderList = new String[]{
            "19-og-under",
            "20-24",
            "25-29",
            "30-39",
            "40-49",
            "50-59",
            "60-66",
            "67-70"
    };

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Test
    public void skalGjoreMappingFraFacetFieldTilFacetResultsKorrekt() {
        FacetField.Count count1 = mock(FacetField.Count.class);
        FacetField.Count count2 = mock(FacetField.Count.class);
        FacetField.Count count3 = mock(FacetField.Count.class);
        when(count1.getName()).thenReturn("X111111");
        when(count2.getName()).thenReturn("Y444444");
        when(count3.getName()).thenReturn("Z777777");
        when(count1.getCount()).thenReturn(10L);
        when(count2.getCount()).thenReturn(20L);
        when(count3.getCount()).thenReturn(30L);
        List<FacetField.Count> values = new ArrayList<>();
        values.add(count1);
        values.add(count2);
        values.add(count3);
        FacetField facetField = mock(FacetField.class);
        when(facetField.getValues()).thenReturn(values);

        FacetResults facetResults = SolrUtils.mapFacetResults(facetField);

        assertThat(facetResults.getFacetResults().get(0).getValue()).isEqualTo("X111111");
        assertThat(facetResults.getFacetResults().get(1).getValue()).isEqualTo("Y444444");
        assertThat(facetResults.getFacetResults().get(2).getValue()).isEqualTo("Z777777");
        assertThat(facetResults.getFacetResults().get(0).getCount()).isEqualTo(10L);
        assertThat(facetResults.getFacetResults().get(1).getCount()).isEqualTo(20L);
        assertThat(facetResults.getFacetResults().get(2).getCount()).isEqualTo(30L);
    }

    @Test
    public void skalReturnereEnSolrQueryMedRiktigeParametereSatt() {
        String query = "id: id AND value: value";
        String facetField = "value";

        SolrQuery solrQuery = SolrUtils.buildSolrFacetQuery(query, facetField);

        assertThat(solrQuery.getQuery()).isEqualTo(query);
        assertThat(solrQuery.getFacetFields()[0]).isEqualTo("value");
        assertThat(Boolean.parseBoolean(solrQuery.get("facet"))).isEqualTo(true);
    }

    @Test
    public void skalKorrektAvgjoreOmErSlaveNode() throws Exception {
        System.setProperty("cluster.ismasternode", "false");
        assertTrue(SolrUtils.isSlaveNode());
        System.setProperty("cluster.ismasternode", "true");
        assertFalse(SolrUtils.isSlaveNode());
    }

    @Test
    public void skalKasteExceptionHvisStatusIkkeErNull() throws Exception {
        expectedException.expect(SolrUpdateResponseCodeException.class);
        SolrUtils.checkSolrResponseCode(1);
    }

    @Test
    public void skalByggSolrQueryMedInaktiveBrukere() throws Exception {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.ferdigfilterListe.add(Brukerstatus.INAKTIVE_BRUKERE);
        String inaktiveBrukereFilter = "(formidlingsgruppekode:ISERV)";
        String enhetId = "0713";
        String queryString = "enhet_id:" + enhetId;

        SolrQuery query = SolrUtils.buildSolrQuery(queryString, false,"","", filtervalg);
        assertThat(query.getFilterQueries()).contains("enhet_id:" + enhetId);
        assertThat(query.getFilterQueries()).contains(inaktiveBrukereFilter);

    }

    @Test
    public void skalByggSolrQueryMedNyeBrukere() throws Exception {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.ferdigfilterListe.add(Brukerstatus.UTFORDELTE_BRUKERE);
        String utfordelteBrukereFilter = "(ny_for_enhet:true)";
        String enhetId = "0713";
        String queryString = "enhet_id:" + enhetId;

        SolrQuery query = SolrUtils.buildSolrQuery(queryString, false,"","", filtervalg);
        assertThat(query.getFilterQueries()).contains("enhet_id:" + enhetId);
        assertThat(query.getFilterQueries()).contains(utfordelteBrukereFilter);
    }

    @Test
    public void skalByggSolrQueryMedNyeBrukereOgInaktiveBrukere() throws Exception {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.ferdigfilterListe = asList(Brukerstatus.UTFORDELTE_BRUKERE, Brukerstatus.INAKTIVE_BRUKERE);
        String forventetFilter = "(ny_for_enhet:true AND formidlingsgruppekode:ISERV)";
        String enhetId = "0713";
        String queryString = "enhet_id:" + enhetId;

        SolrQuery query = SolrUtils.buildSolrQuery(queryString, false,"","", filtervalg);
        assertThat(query.getFilterQueries()).contains("enhet_id:" + enhetId);
        assertThat(query.getFilterQueries()).contains(forventetFilter);
    }

    @Test
    public void skalBygeSorterPaVeileder() throws Exception {
        Filtervalg filtervalg = new Filtervalg();
        SolrQuery query = SolrUtils.buildSolrQuery("", true,"","", filtervalg);
        assertThat(query.getSortField()).contains("ny_for_veileder desc");
    }

    @Test
    public void skalLeggeTilAlderFilterISolrQuery() {
        String PREFIX = "fodselsdato:[NOW/DAY-";
        String POSTFIX = "+1DAY/DAY]";

        assertThat(SolrUtils.alderFilter("19-og-under")).isEqualTo(PREFIX + "20YEARS+1DAY TO NOW" + POSTFIX);
        assertThat(SolrUtils.alderFilter("20-24")).isEqualTo(PREFIX + "25YEARS+1DAY TO NOW-20YEARS" + POSTFIX);
        assertThat(SolrUtils.alderFilter("25-29")).isEqualTo(PREFIX + "30YEARS+1DAY TO NOW-25YEARS" + POSTFIX);
        assertThat(SolrUtils.alderFilter("30-39")).isEqualTo(PREFIX + "40YEARS+1DAY TO NOW-30YEARS" + POSTFIX);
        assertThat(SolrUtils.alderFilter("40-49")).isEqualTo(PREFIX + "50YEARS+1DAY TO NOW-40YEARS" + POSTFIX);
        assertThat(SolrUtils.alderFilter("50-59")).isEqualTo(PREFIX + "60YEARS+1DAY TO NOW-50YEARS" + POSTFIX);
        assertThat(SolrUtils.alderFilter("60-66")).isEqualTo(PREFIX + "67YEARS+1DAY TO NOW-60YEARS" + POSTFIX);
        assertThat(SolrUtils.alderFilter("67-70")).isEqualTo(PREFIX + "71YEARS+1DAY TO NOW-67YEARS" + POSTFIX);
    }

    @Test
    public void orStatementSkalORETingSammen() {
        List<String> filter = asList("abba", "acdc");
        Function<String, String> mapper = String::toUpperCase;

        String query = orStatement(filter, mapper);

        assertThat(query).isEqualTo("ABBA OR ACDC");
    }

    @Test
    public void skalLeggeTilKjonnFilter() {
        Filtervalg filtervalg = new Filtervalg();
        SolrQuery solrQuery;
        filtervalg.kjonn = singletonList(Kjonn.M);
        solrQuery = SolrUtils.buildSolrQuery("", false,"","", filtervalg);

        assertThat(solrQuery.getFilterQueries()).contains("(kjonn:M)");

        filtervalg.kjonn = singletonList(Kjonn.K);
        solrQuery = SolrUtils.buildSolrQuery("",false,"","", filtervalg);
        assertThat(solrQuery.getFilterQueries()).contains("(kjonn:K)");

        filtervalg = new Filtervalg();
        solrQuery = SolrUtils.buildSolrQuery("",false,"","", filtervalg);
        assertThat(solrQuery.getFilterQueries()).containsOnly("");
    }

    @Test
    public void skalIkkeLeggePaaFilterQueryHvisIngenFiltervalgErSatt() {
        Filtervalg filtervalg = new Filtervalg();
        SolrQuery query = SolrUtils.buildSolrQuery("enhet_id:0104",false,"","", filtervalg);
        filtervalg.harAktiveFilter();
        assertThat(query.getFilterQueries()).containsOnly("enhet_id:0104");
    }

    @Test
    public void skalLeggeTilSpesifikkYtelseFilter() throws Exception {
        Filtervalg filter = new Filtervalg();
        filter.ytelse = YtelseFilter.DAGPENGER_MED_PERMITTERING;

        assertThat(filter.harAktiveFilter()).isTrue();
        assertThat(SolrUtils.buildSolrQuery("",false,"","", filter).getFilterQueries()).contains("(ytelse:DAGPENGER_MED_PERMITTERING)");
    }

    @Test
    public void skalLeggeTilGruppeYtelseFilter() throws Exception {
        Filtervalg filter = new Filtervalg();
        filter.ytelse = YtelseFilter.DAGPENGER;

        assertThat(filter.harAktiveFilter()).isTrue();
        assertThat(SolrUtils.buildSolrQuery("",false,"","", filter).getFilterQueries()).contains(
                "(ytelse:ORDINARE_DAGPENGER OR ytelse:DAGPENGER_MED_PERMITTERING OR ytelse:DAGPENGER_OVRIGE)"
        );
    }

    @Test
    public void skalLeggeTilInnsatsgruppeFilter() {
        String prefix = "kvalifiseringsgruppekode:";

        assertThat(SolrUtils.innsatsgruppeFilter(Innsatsgruppe.BATT)).isEqualTo(prefix + Innsatsgruppe.BATT);
    }

    @Test
    public void skalLeggeTilFormidlingsgruppeFilter() {
        String prefix = "formidlingsgruppekode:";

        assertThat(SolrUtils.formidlingsgruppeFilter(Formidlingsgruppe.ARBS)).isEqualTo(prefix + Formidlingsgruppe.ARBS);
    }

    @Test
    public void skalLeggeTilservicegruppeFilter() {
        String prefix = "kvalifiseringsgruppekode:";

        assertThat(SolrUtils.servicegruppeFilter(Servicegruppe.BKART)).isEqualTo(prefix + Servicegruppe.BKART);
    }

    @Test
    public void skalLeggeTilTiltaksfiler() {
        List<String> tiltakstyper = asList("tiltak1", "tiltak2");

        Filtervalg filtervalg = new Filtervalg().setTiltakstyper(tiltakstyper);

        SolrQuery solrQuery = SolrUtils.buildSolrQuery("",false,"","", filtervalg);
        assertThat(solrQuery.getFilterQueries()).contains("(tiltak:tiltak1 OR tiltak:tiltak2)");
    }

    @Test
    public void skalLeggeTilTiltakJa() {
        Map<String, AktivitetFiltervalg> aktivitetFiltervalg = new HashMap<>();
        aktivitetFiltervalg.put(TILTAK, AktivitetFiltervalg.JA);
        SolrQuery solrQuery = SolrUtils.buildSolrQuery("",false,"","", new Filtervalg().setAktiviteter(aktivitetFiltervalg));

        assertThat(solrQuery.getFilterQueries()).contains("(tiltak:*)");
    }

    @Test
    public void skalLeggeTilTiltakNei() {
        Map<String, AktivitetFiltervalg> aktivitetFiltervalg = new HashMap<>();
        aktivitetFiltervalg.put(TILTAK, AktivitetFiltervalg.NEI);
        SolrQuery solrQuery = SolrUtils.buildSolrQuery("",false,"","", new Filtervalg().setAktiviteter(aktivitetFiltervalg));

        assertThat(solrQuery.getFilterQueries()).contains("(*:* AND -tiltak:*)");
    }

    @Test
    public void skalIkkeLeggeTilTiltakJaEllerNei() {
        Map<String, AktivitetFiltervalg> aktivitetFiltervalg = new HashMap<>();
        aktivitetFiltervalg.put(TILTAK, AktivitetFiltervalg.NA);
        SolrQuery solrQuery = SolrUtils.buildSolrQuery("",false,"","", new Filtervalg().setAktiviteter(aktivitetFiltervalg));

        asList(solrQuery.getFilterQueries()).forEach( (filter) -> assertThat(filter).doesNotContain("tiltak"));
    }

    @Test
    public void skalLeggeTilAktiviteter() {
        Map<String, AktivitetFiltervalg> aktivitetFiltervalg = new HashMap<>();
        aktivitetFiltervalg.put("aktivitet1", AktivitetFiltervalg.JA);
        aktivitetFiltervalg.put("aktivitet2", AktivitetFiltervalg.NEI);

        SolrQuery solrQuery = SolrUtils.buildSolrQuery("",false,"","", new Filtervalg().setAktiviteter(aktivitetFiltervalg));

        assertThat(solrQuery.getFilterQueries()).contains("(aktiviteter:aktivitet1) AND (*:* AND -aktiviteter:aktivitet2)");
    }



}
