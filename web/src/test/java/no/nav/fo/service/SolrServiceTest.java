package no.nav.fo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrServiceTest {

    private SolrService solrService = new SolrService();

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
    public void skalReturnereNullHvisDatoenErNull() {
        assertThat(solrService.parseDato(null)).isEqualTo(null);
    }

    @Test
    public void skalReturnereNullHvisDatoenErUfullstendig() {
        assertThat(solrService.parseDato("TZ")).isEqualTo(null);
    }

    @Test
    public void skalReturnereDatostrengenHvisDatoenErOk() {
        Object dato = "2017-01-01'T'23:23:23.23'Z'";
        assertThat(solrService.parseDato(dato)).isEqualTo(dato);
    }

    @Test
    public void skalKorrektAvgjoreOmErSlaveNode() throws Exception {
        System.setProperty("cluster.ismasternode", "false");
        assertTrue(SolrService.isSlaveNode());
        System.setProperty("cluster.ismasternode", "true");
        assertFalse(SolrService.isSlaveNode());
    }
}

