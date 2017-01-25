package no.nav.fo.service;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SolrServiceTest {
    @Test
    public void shouldCorrectlyDetermineIfSlaveNode() throws Exception {
        System.setProperty("cluster.ismasternode", "false");
        assertTrue(SolrService.isSlaveNode());
        System.setProperty("cluster.ismasternode", "true");
        assertFalse(SolrService.isSlaveNode());
    }
}