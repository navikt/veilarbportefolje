package no.nav.fo.service;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.solr.client.solrj.SolrQuery.ORDER.asc;
import static org.apache.solr.client.solrj.SolrQuery.ORDER.desc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SolrServiceTest {

    private SolrService solrService;

    @Rule
    public ExpectedException expectedException =ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        solrService = new SolrService();
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

        Map<String, Object> nyesteBruker = solrService.nyesteBruker(brukere);

        assertThat(nyesteBruker).isEqualTo(bruker3);
    }

    @Test
    public void skalKorrektAvgjoreOmErSlaveNode() throws Exception {
        System.setProperty("cluster.ismasternode", "false");
        assertTrue(SolrService.isSlaveNode());
        System.setProperty("cluster.ismasternode", "true");
        assertFalse(SolrService.isSlaveNode());
    }

    @Test
    public void skalKasteExceptionHvisStatusIkkeErNull() throws Exception {
        expectedException.expect(SolrUpdateResponseCodeException.class);
        solrService.checkSolrResponseCode(1);
    }

    @Test
    public void skalByggSolrQueryMedAlleFelterUtfylt() throws Exception {
        String enhetId = "0713";
        SolrQuery query = solrService.buildSolrQuery(enhetId, "ascending");
        assertThat(query.getQuery()).contains(enhetId);
        assertThat(query.getSortField().contains("fornavn")).isTrue();
        assertThat(query.getSortField().contains("etternavn")).isTrue();
        assertThat(query.getSorts().get(0).getOrder()).isEqualTo(asc);
    }

    @Test
    public void skalByggeSolrQueryMedDescendingSort() throws Exception {
        String enhetId = "0713";
        SolrQuery query = solrService.buildSolrQuery(enhetId, "descending");
        assertThat(query.getSorts().get(0).getOrder()).isEqualTo(desc);
    }
}

