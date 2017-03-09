package no.nav.fo.util;

import no.nav.fo.domene.FacetResults;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.service.SolrUpdateResponseCodeException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SolrUtilsTest {

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
    public void skalFinneNyesteBruker() {
        List<Map<String, Object>> brukere = new ArrayList<>();
        Map<String, Object> bruker1 = new HashMap<>();
        Map<String, Object> bruker2 = new HashMap<>();
        Map<String, Object> bruker3 = new HashMap<>();
        bruker1.put("tidsstempel", new Date(System.currentTimeMillis()));
        bruker2.put("tidsstempel", new Date(System.currentTimeMillis() + 100000));
        bruker3.put("tidsstempel", new Date(System.currentTimeMillis() + 10000000));
        brukere.add(bruker1);
        brukere.add(bruker2);
        brukere.add(bruker3);

        Map<String, Object> nyesteBruker = SolrUtils.nyesteBruker(brukere);

        assertThat(nyesteBruker).isEqualTo(bruker3);
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
        filtervalg.inaktiveBrukere = true;
        String inaktiveBrukereFilter = "(formidlingsgruppekode:ISERV AND veileder_id:*)";
        String enhetId = "0713";
        String queryString = "enhet_id:"+enhetId;

        SolrQuery query = SolrUtils.buildSolrQuery(queryString, filtervalg);
        assertThat(query.getFilterQueries()).contains("enhet_id:" + enhetId);
        assertThat(query.getFilterQueries()).contains(inaktiveBrukereFilter);
    }

    @Test
    public void skalLeggeTilAlderFilterISolrQuery() {
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.alder = 1;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo("fodselsdato:[NOW-19YEARS TO NOW]");

        filtervalg.alder = 2;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo("fodselsdato:[NOW-24YEARS TO NOW-20YEARS]");

        filtervalg.alder = 3;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo("fodselsdato:[NOW-29YEARS TO NOW-25YEARS]");

        filtervalg.alder = 4;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo("fodselsdato:[NOW-39YEARS TO NOW-30YEARS]");

        filtervalg.alder = 5;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo("fodselsdato:[NOW-49YEARS TO NOW-40YEARS]");

        filtervalg.alder = 6;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo("fodselsdato:[NOW-59YEARS TO NOW-50YEARS]");

        filtervalg.alder = 7;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo("fodselsdato:[NOW-66YEARS TO NOW-60YEARS]");

        filtervalg.alder = 8;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo("fodselsdato:[NOW-70YEARS TO NOW-67YEARS]");

    }

    @Test
    public void skalLeggeTilKjonnFilter() {
        Filtervalg filtervalg = new Filtervalg();
        SolrQuery solrQuery;
        filtervalg.kjonn = "M";
        solrQuery = SolrUtils.buildSolrQuery("", filtervalg);

        assertThat(solrQuery.getFilterQueries()).contains("kjonn:M");

        filtervalg.kjonn = "K";
        solrQuery = SolrUtils.buildSolrQuery("", filtervalg);
        assertThat(solrQuery.getFilterQueries()).contains("kjonn:K");
    }

    @Test
    public void skalIkkeLeggePaaFilterQueryHvisIngenFiltervalgErSatt() {
        Filtervalg filtervalg = new Filtervalg();
        SolrQuery query = SolrUtils.buildSolrQuery("enhet_id:0104", filtervalg);
        filtervalg.harAktiveFilter();
        assertThat(query.getFilterQueries()).containsOnly("enhet_id:0104");
    }
}
