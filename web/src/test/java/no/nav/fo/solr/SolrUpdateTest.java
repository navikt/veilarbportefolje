package no.nav.fo.solr;

import lombok.SneakyThrows;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.dialogarena.config.fasit.FasitUtils;
import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.ArbeidslisteRepository;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.PersonId;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.SolrService;
import no.nav.fo.service.SolrServiceImpl;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

import static java.util.Arrays.asList;
import static no.nav.dialogarena.config.DevelopmentSecurity.setupIntegrationTestSecurity;
import static no.nav.dialogarena.smoketest.Tag.SMOKETEST;
import static no.nav.fo.config.LocalJndiContextConfig.setupDataSourceWithCredentials;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Tag(SMOKETEST)
@SuppressWarnings("Duplicates")
public class SolrUpdateTest {
    private static SolrClient solrClient;
    private static String VEILARBPORTEFOLJE = "veilarbportefolje";

    private static final PersonId AREMARK_PERSON_ID = PersonId.of("3125339");
    private static DataSource ds;
    private static JdbcTemplate jdbcTemplate;
    private static NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static BrukerRepository brukerRepository;
    private static SolrService solrService;

    @BeforeEach
    public void setup() {
        setupIntegrationTestSecurity(new DevelopmentSecurity.IntegrationTestConfig(VEILARBPORTEFOLJE));
        Properties properties = FasitUtils.getApplicationEnvironment(VEILARBPORTEFOLJE);
        solrClient = new HttpSolrClient.Builder()
                    .withBaseSolrUrl(properties.getProperty("veilarbportefolje.solr.masternode"))
                    .withHttpClient(createHttpClientForSolr())
                    .build();

        ds =  setupDataSourceWithCredentials(FasitUtils.getDbCredentials(VEILARBPORTEFOLJE));
        jdbcTemplate = new JdbcTemplate(ds);
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds);
        brukerRepository = new BrukerRepository(jdbcTemplate, ds, namedParameterJdbcTemplate);
        solrService = new SolrServiceImpl(solrClient, mock(SolrClient.class),brukerRepository,
                mock(ArbeidslisteRepository.class),mock(AktoerService.class), mock(AktivitetDAO.class));
        }

    @Test
    @SneakyThrows
    public void skalOppdatereSolrIndeksKorrekt() {
        SolrInputDocument solrInputDocument = brukerRepository.retrieveBrukeremedBrukerdata(asList(AREMARK_PERSON_ID)).get(0);
        solrClient.add(solrInputDocument);
        solrClient.commit();

        SolrQuery solrQuery = new SolrQuery("person_id:"+AREMARK_PERSON_ID.toString());
        QueryResponse response = solrClient.query(solrQuery);
        SolrDocument solrDocument = response.getResults().get(0);
        assertThat(solrDocument.getFieldNames()).containsAll(solrDocument.getFieldNames());

        solrService.slettBruker(AREMARK_PERSON_ID);
        solrService.commit();
        QueryResponse responseSlettet = solrClient.query(solrQuery);
        assertThat(responseSlettet.getResults()).isEmpty();
    }

    private HttpClient createHttpClientForSolr() {
        String username = System.getProperty("no.nav.modig.security.systemuser.username");
        String password = System.getProperty("no.nav.modig.security.systemuser.password");

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        return HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .addInterceptorFirst(new PreemptiveAuthInterceptor())
                .build();
    }

    private class PreemptiveAuthInterceptor implements HttpRequestInterceptor {
        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            AuthState authState = (AuthState) context.getAttribute(HttpClientContext.TARGET_AUTH_STATE);
            if (authState.getAuthScheme() == null) {
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(HttpClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
                Credentials credentials = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                if (credentials == null) {
                    throw new HttpException("No credentials provided for preemptive authentication.");
                }
                authState.update(new BasicScheme(), credentials);
            }

        }
    }
}
