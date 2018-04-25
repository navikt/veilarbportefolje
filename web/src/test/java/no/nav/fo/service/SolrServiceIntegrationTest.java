package no.nav.fo.service;

import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.config.ApplicationConfigTest;
import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.util.sql.SqlUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.util.NamedList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Date;

import static no.nav.fo.config.RemoteFeatureConfig.*;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class SolrServiceIntegrationTest {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private BrukerRepository brukerRepository;

    @Before
    public void deleteData() {
        jdbcTemplate.execute("truncate table oppfolgingsbruker");
        jdbcTemplate.execute("truncate table metadata");
    }

    @Test
    public void skalSletteBrukerVedDeltaindeksering() throws IOException, SolrServerException {
        System.setProperty("cluster.ismasternode", "true");

        insertISERVuser();
        SolrClient solrClientMaster = mock(SolrClient.class);
        SolrClient solrClientSlave = mock(SolrClient.class);
        ArbeidslisteRepository arbeidslisteRepository = mock(ArbeidslisteRepository.class);
        AktoerService aktoerService = mock(AktoerService.class);
        AktivitetDAO aktivitetDAO = mock(AktivitetDAO.class);
        VeilederService veilederService = mock(VeilederService.class);
        FlyttSomNyeFeature flyttSomNyeFeature = mock(FlyttSomNyeFeature.class);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        SolrService solrService = new SolrServiceImpl(solrClientMaster, solrClientSlave, brukerRepository, arbeidslisteRepository, aktoerService, veilederService, aktivitetDAO, flyttSomNyeFeature);


        UpdateResponse response = new UpdateResponse();
        response.setResponse(new NamedList<>());
        when(solrClientMaster.deleteByQuery(any())).thenReturn(response);

        solrService.deltaindeksering();

        verify(solrClientMaster, times(1)).deleteByQuery(captor.capture());
        verify(solrClientMaster, times(1)).commit();

        assertThat(captor.getValue()).isEqualTo("fnr:11111111111");
    }

    private void insertISERVuser() {
        SqlUtils.insert(jdbcTemplate, "OPPFOLGINGSBRUKER")
            .value("PERSON_ID", 1234)
            .value("FODSELSNR", "11111111111")
            .value("FORMIDLINGSGRUPPEKODE", "ISERV")
            .value("KVALIFISERINGSGRUPPEKODE", "VARIG")
            .value("TIDSSTEMPEL", new Date())
            .execute();

        SqlUtils.insert(jdbcTemplate, "METADATA")
            .value("SIST_INDEKSERT", new Date(0))
            .execute();
    }
}
