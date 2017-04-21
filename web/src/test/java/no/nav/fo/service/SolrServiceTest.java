package no.nav.fo.service;

import no.nav.fo.database.BrukerRepository;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SolrServiceTest {

    @Mock
    JdbcTemplate db;
    @Mock
    BrukerRepository brukerRepository;
    @Mock
    private SolrClient solrClientSlave;
    @Mock
    private SolrClient solrClientMaster;

    @InjectMocks
    SolrService service;


    @Test
    public void deltaindekseringSkalOppdatereTidsstempel() throws Exception {
        when(brukerRepository.retrieveOppdaterteBrukere()).thenReturn(asList(
                new SolrInputDocument()
        ));
        System.setProperty("cluster.ismasternode", "true");

        service.deltaindeksering();

        verify(brukerRepository, atLeastOnce()).updateTidsstempel(any(Timestamp.class));
    }

    @Test
    public void deltaindekseringSkalIkkeOppdatereTidsstempel() throws Exception {
        when(brukerRepository.retrieveOppdaterteBrukere()).thenReturn(asList());
        System.setProperty("cluster.ismasternode", "true");

        service.deltaindeksering();

        verify(brukerRepository, never()).updateTidsstempel(any(Timestamp.class));
    }
}