package no.nav.fo.util;

import no.nav.fo.domene.Bruker;
import no.nav.fo.domene.FacetResults;
import no.nav.fo.domene.Filtervalg;
import no.nav.fo.service.SolrUpdateResponseCodeException;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static no.nav.fo.util.SolrUtils.*;
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
    public void skalSammenligneEtternavnRiktig() {
        Bruker bruker1 = new Bruker().setEtternavn("Andersen");
        Bruker bruker2 = new Bruker().setEtternavn("Anderson");
        Bruker bruker3 = new Bruker().setEtternavn("Davidsen");

        Comparator<Bruker> comparator = brukerNavnComparator();
        int compared1 = comparator.compare(bruker1, bruker2);
        int compared2 = comparator.compare(bruker2, bruker1);
        int compared3 = comparator.compare(bruker1, bruker3);
        int compared4 = comparator.compare(bruker3, bruker2);

        assertThat(compared1).isEqualTo(-1);
        assertThat(compared2).isEqualTo(1);
        assertThat(compared3).isEqualTo(-1);
        assertThat(compared4).isEqualTo(1);
    }

    @Test
    public void skalSammenligneFornavnRiktigNarEtternavnErLike() {
        Bruker bruker1 = new Bruker().setEtternavn("Andersen").setFornavn("Anders");
        Bruker bruker2 = new Bruker().setEtternavn("Andersen").setFornavn("Anders");
        Bruker bruker3 = new Bruker().setEtternavn("Andersen").setFornavn("Petter");
        Bruker bruker4 = new Bruker().setEtternavn("Andersen").setFornavn("Jakob");

        Comparator<Bruker> comparator = brukerNavnComparator();
        int compared1 = comparator.compare(bruker1, bruker3);
        int compared2 = comparator.compare(bruker3, bruker4);
        int compared3 = comparator.compare(bruker1, bruker2);

        assertThat(compared1).isEqualTo(-1);
        assertThat(compared2).isEqualTo(1);
        assertThat(compared3).isEqualTo(0);
    }

    @Test
    public void skalSammenligneNorskeBokstaverRiktig() {
        Bruker bruker1 = new Bruker().setEtternavn("Ære").setFornavn("Åge");
        Bruker bruker2 = new Bruker().setEtternavn("Øvrebø").setFornavn("Ærling");
        Bruker bruker3 = new Bruker().setEtternavn("Åre").setFornavn("Øystein");
        Bruker bruker4 = new Bruker().setEtternavn("Åre").setFornavn("Øystein");
        Bruker bruker5 = new Bruker().setEtternavn("Zigzag").setFornavn("Øystein");
        Bruker bruker6 = new Bruker().setEtternavn("Øvrebø").setFornavn("Åge");


        Comparator<Bruker> comparator = brukerNavnComparator();
        int compared1 = comparator.compare(bruker1, bruker2);
        int compared2 = comparator.compare(bruker2, bruker3);
        int compared3 = comparator.compare(bruker3, bruker1);
        int compared4 = comparator.compare(bruker3, bruker4);
        int compared5 = comparator.compare(bruker5, bruker1);
        int compared6 = comparator.compare(bruker2, bruker6);
        int compared7 = comparator.compare(bruker6, bruker2);

        assertThat(compared1).isEqualTo(-1);
        assertThat(compared2).isEqualTo(-1);
        assertThat(compared3).isEqualTo(1);
        assertThat(compared4).isEqualTo(0);
        assertThat(compared5).isEqualTo(-1);
        assertThat(compared6).isEqualTo(-1);
        assertThat(compared7).isEqualTo(1);
    }

    @Test
    public void skalSammenligneDobbelARiktig() {
        Bruker bruker1 = new Bruker().setEtternavn("Aakesen");
        Bruker bruker2 = new Bruker().setEtternavn("Aresen");
        Bruker bruker3 = new Bruker().setEtternavn("Ågesen");

        Comparator<Bruker> comparator = brukerNavnComparator();
        int compared1 = comparator.compare(bruker1, bruker2);
        int compared2 = comparator.compare(bruker1, bruker3);

        assertThat(compared1).isEqualTo(1);
        assertThat(compared2).isEqualTo(1);
    }

    @Test
    public void skalSetteRiktigSortOrderNarDenErAscending() {
        Bruker bruker1 = new Bruker().setEtternavn("Andersen").setFornavn("Anders");
        Bruker bruker2 = new Bruker().setEtternavn("Pettersen").setFornavn("Anders");
        Bruker bruker3 = new Bruker().setEtternavn("Davidsen").setFornavn("Petter");
        Bruker bruker4 = new Bruker().setEtternavn("Andersen").setFornavn("Jakob");
        Bruker bruker5 = new Bruker().setEtternavn("Andersen").setFornavn("Abel");
        Bruker bruker6 = new Bruker().setEtternavn("Andersen").setFornavn("Abel");

        Comparator<Bruker> comparator = setComparatorSortOrder(brukerNavnComparator(), "ascending");
        int compared1 = comparator.compare(bruker1, bruker2);
        int compared2 = comparator.compare(bruker2, bruker3);
        int compared3 = comparator.compare(bruker1, bruker4);
        int compared4 = comparator.compare(bruker1, bruker5);
        int compared5 = comparator.compare(bruker6, bruker5);

        assertThat(compared1).isEqualTo(-1);
        assertThat(compared2).isEqualTo(1);
        assertThat(compared3).isEqualTo(-1);
        assertThat(compared4).isEqualTo(1);
        assertThat(compared5).isEqualTo(0);
    }

    @Test
    public void skalSetteRiktigSortOrderNarDenErDescending() {
        Bruker bruker1 = new Bruker().setEtternavn("Andersen").setFornavn("Anders");
        Bruker bruker2 = new Bruker().setEtternavn("Pettersen").setFornavn("Anders");
        Bruker bruker3 = new Bruker().setEtternavn("Davidsen").setFornavn("Petter");
        Bruker bruker4 = new Bruker().setEtternavn("Andersen").setFornavn("Jakob");
        Bruker bruker5 = new Bruker().setEtternavn("Andersen").setFornavn("Abel");
        Bruker bruker6 = new Bruker().setEtternavn("Andersen").setFornavn("Abel");

        Comparator<Bruker> comparator = setComparatorSortOrder(brukerNavnComparator(), "descending");
        int compared1 = comparator.compare(bruker1, bruker2);
        int compared2 = comparator.compare(bruker2, bruker3);
        int compared3 = comparator.compare(bruker1, bruker4);
        int compared4 = comparator.compare(bruker1, bruker5);
        int compared5 = comparator.compare(bruker6, bruker5);

        assertThat(compared1).isEqualTo(1);
        assertThat(compared2).isEqualTo(-1);
        assertThat(compared3).isEqualTo(1);
        assertThat(compared4).isEqualTo(-1);
        assertThat(compared5).isEqualTo(0);
    }

    @Test
    public void skalSammenligneNyeOgGamleBrukereRiktig() {
        // Definisjonen av nye brukere: veilederId == null

        Bruker bruker1 = new Bruker().setVeilederId("x");
        Bruker bruker2 = new Bruker().setVeilederId("y");
        Bruker bruker3 = new Bruker().setVeilederId(null);
        Bruker bruker4 = new Bruker().setVeilederId(null);

        Comparator<Bruker> comparator = brukerErNyComparator();

        int compared1 = comparator.compare(bruker1, bruker3);
        int compared2 = comparator.compare(bruker4, bruker2);
        int compared3 = comparator.compare(bruker1, bruker2);
        int compared4 = comparator.compare(bruker3, bruker4);

        assertThat(compared1).isEqualTo(1);
        assertThat(compared2).isEqualTo(-1);
        assertThat(compared3).isEqualTo(0);
        assertThat(compared4).isEqualTo(0);
    }

    @Test
    public void skalIkkeSortere() {
        Bruker bruker1 = new Bruker().setVeilederId("x");
        Bruker bruker2 = new Bruker().setVeilederId(null).setEtternavn("Nilsen");
        Bruker bruker3 = new Bruker().setVeilederId(null).setEtternavn("Johnsen");
        Bruker bruker4 = new Bruker().setVeilederId("y");
        List<Bruker> brukere = new ArrayList<>();
        brukere.add(bruker1);
        brukere.add(bruker2);
        brukere.add(bruker3);
        brukere.add(bruker4);

        List<Bruker> brukereSortert = sortBrukere(brukere, "ikke_satt", null);

        assertThat(brukereSortert.get(0)).isEqualTo(bruker1);
        assertThat(brukereSortert.get(1)).isEqualTo(bruker2);
        assertThat(brukereSortert.get(2)).isEqualTo(bruker3);
        assertThat(brukereSortert.get(3)).isEqualTo(bruker4);
    }

    @Test
    public void skalKunSortereNyeBrukereOverst() {
        Bruker bruker1 = new Bruker().setVeilederId("x");
        Bruker bruker2 = new Bruker().setVeilederId(null).setEtternavn("Nilsen");
        Bruker bruker3 = new Bruker().setVeilederId(null).setEtternavn("Johnsen");
        Bruker bruker4 = new Bruker().setVeilederId("y");
        List<Bruker> brukere = new ArrayList<>();
        brukere.add(bruker1);
        brukere.add(bruker2);
        brukere.add(bruker3);
        brukere.add(bruker4);

        List<Bruker> brukereSortert = sortBrukere(brukere, "ikke_satt", brukerErNyComparator());

        assertThat(brukereSortert.get(0)).isEqualTo(bruker2);
        assertThat(brukereSortert.get(1)).isEqualTo(bruker3);
        assertThat(brukereSortert.get(2)).isEqualTo(bruker1);
        assertThat(brukereSortert.get(3)).isEqualTo(bruker4);
    }

    @Test
    public void skalKunSorterePaaNavn() {
        Bruker bruker1 = new Bruker().setVeilederId("x").setEtternavn("Abel");
        Bruker bruker2 = new Bruker().setVeilederId(null).setEtternavn("Nilsen");
        Bruker bruker3 = new Bruker().setVeilederId(null).setEtternavn("Johnsen");
        Bruker bruker4 = new Bruker().setVeilederId("y").setEtternavn("Bro");
        List<Bruker> brukere = new ArrayList<>();
        brukere.add(bruker1);
        brukere.add(bruker2);
        brukere.add(bruker3);
        brukere.add(bruker4);

        List<Bruker> brukereSortert = sortBrukere(brukere, "ascending", null);

        assertThat(brukereSortert.get(0)).isEqualTo(bruker1);
        assertThat(brukereSortert.get(1)).isEqualTo(bruker4);
        assertThat(brukereSortert.get(2)).isEqualTo(bruker3);
        assertThat(brukereSortert.get(3)).isEqualTo(bruker2);
    }

    @Test
    public void skalSorterePaaNyeBrukereOgNavnAscending() {
        Bruker bruker1 = new Bruker().setVeilederId("x").setEtternavn("Abel");
        Bruker bruker2 = new Bruker().setVeilederId(null).setEtternavn("Nilsen");
        Bruker bruker3 = new Bruker().setVeilederId(null).setEtternavn("Johnsen");
        Bruker bruker4 = new Bruker().setVeilederId("y").setEtternavn("Bro");
        List<Bruker> brukere = new ArrayList<>();
        brukere.add(bruker1);
        brukere.add(bruker2);
        brukere.add(bruker3);
        brukere.add(bruker4);

        List<Bruker> brukereSortert = sortBrukere(brukere, "ascending", brukerErNyComparator());

        assertThat(brukereSortert.get(0)).isEqualTo(bruker3);
        assertThat(brukereSortert.get(1)).isEqualTo(bruker2);
        assertThat(brukereSortert.get(2)).isEqualTo(bruker1);
        assertThat(brukereSortert.get(3)).isEqualTo(bruker4);
    }

    @Test
    public void skalSorterePaaNyeBrukereOgNavnDescending() {
        Bruker bruker1 = new Bruker().setVeilederId("x").setEtternavn("Abel");
        Bruker bruker2 = new Bruker().setVeilederId(null).setEtternavn("Nilsen");
        Bruker bruker3 = new Bruker().setVeilederId(null).setEtternavn("Johnsen");
        Bruker bruker4 = new Bruker().setVeilederId("y").setEtternavn("Bro");
        List<Bruker> brukere = new ArrayList<>();
        brukere.add(bruker1);
        brukere.add(bruker2);
        brukere.add(bruker3);
        brukere.add(bruker4);

        List<Bruker> brukereSortert = sortBrukere(brukere, "descending", brukerErNyComparator());

        assertThat(brukereSortert.get(0)).isEqualTo(bruker2);
        assertThat(brukereSortert.get(1)).isEqualTo(bruker3);
        assertThat(brukereSortert.get(2)).isEqualTo(bruker4);
        assertThat(brukereSortert.get(3)).isEqualTo(bruker1);
    }

    @Test
    public void skalLeggeTilAlderFilterISolrQuery() {
        Filtervalg filtervalg = new Filtervalg();

        String PREFIX = "fodselsdato:[NOW/DAY-";
        String POSTFIX = "+1DAY/DAY]";

        filtervalg.alder = 1;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo(PREFIX + "20YEARS+1DAY TO NOW" + POSTFIX);

        filtervalg.alder = 2;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo(PREFIX + "25YEARS+1DAY TO NOW-20YEARS" + POSTFIX);

        filtervalg.alder = 3;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo(PREFIX + "30YEARS+1DAY TO NOW-25YEARS" + POSTFIX);

        filtervalg.alder = 4;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo(PREFIX + "40YEARS+1DAY TO NOW-30YEARS" + POSTFIX);

        filtervalg.alder = 5;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo(PREFIX + "50YEARS+1DAY TO NOW-40YEARS" + POSTFIX);

        filtervalg.alder = 6;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo(PREFIX + "60YEARS+1DAY TO NOW-50YEARS" + POSTFIX);

        filtervalg.alder = 7;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo(PREFIX + "67YEARS+1DAY TO NOW-60YEARS" + POSTFIX);

        filtervalg.alder = 8;
        assertThat(SolrUtils.leggTilAlderFilter(filtervalg)).isEqualTo(PREFIX + "71YEARS+1DAY TO NOW-67YEARS" + POSTFIX);

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
