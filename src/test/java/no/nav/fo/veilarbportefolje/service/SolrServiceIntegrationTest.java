package no.nav.fo.veilarbportefolje.service;

import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.indeksering.IndekseringService;
import no.nav.fo.veilarbportefolje.indeksering.SolrService;
import no.nav.fo.veilarbportefolje.mock.LockingTaskExecutorMock;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.sql.SqlUtils;
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

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ApplicationConfigTest.class})
public class SolrServiceIntegrationTest {

    @Inject
    private JdbcTemplate jdbcTemplate;

    @Inject
    private BrukerRepository brukerRepository;

    @Inject
    private PepClient pepClient;

    private LockingTaskExecutor lockingTaskExecutorMock = new LockingTaskExecutorMock();

    @Before
    public void deleteData() {
        jdbcTemplate.execute("truncate table oppfolgingsbruker");
        jdbcTemplate.execute("truncate table metadata");
    }

    @Test
    public void skalSletteBrukerVedDeltaindeksering() throws IOException, SolrServerException {
        insertISERVuser();
        SolrClient solrClientMaster = mock(SolrClient.class);
        SolrClient solrClientSlave = mock(SolrClient.class);
        AktoerService aktoerService = mock(AktoerService.class);
        AktivitetDAO aktivitetDAO = mock(AktivitetDAO.class);
        VeilederService veilederService = mock(VeilederService.class);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        IndekseringService indekseringService = new SolrService(solrClientMaster, solrClientSlave, brukerRepository, aktoerService, veilederService, aktivitetDAO, lockingTaskExecutorMock, pepClient);


        UpdateResponse response = new UpdateResponse();
        response.setResponse(new NamedList<>());
        when(solrClientMaster.deleteByQuery(any())).thenReturn(response);

        indekseringService.deltaindeksering();

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
