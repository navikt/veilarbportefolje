package no.nav.fo.veilarbportefolje.indeksering;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.config.DatabaseConfig;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.service.AktoerService;
import no.nav.fo.veilarbportefolje.service.PepClient;
import no.nav.fo.veilarbportefolje.service.VeilederService;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.util.EnvironmentUtils;
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
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.util.UUID;

import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_PASSWORD;
import static no.nav.brukerdialog.tools.SecurityConstants.SYSTEMUSER_USERNAME;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.*;
import static no.nav.fo.veilarbportefolje.util.PingUtils.ping;
import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.Q;
import static no.nav.sbl.util.EnvironmentUtils.getEnvironmentClass;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@Import({DatabaseConfig.class})
public class IndekseringConfig {

    private static final String URL = getRequiredProperty(VEILARBPORTEFOLJE_SOLR_BRUKERCORE_URL_PROPERTY);
    public static int BATCH_SIZE = 1000;
    public final static int BATCH_SIZE_LIMIT = 1000;
    public static String VEILARBELASTIC_USERNAME = getRequiredProperty(ELASTICSEARCH_USERNAME_PROPERTY);
    public static String VEILARBELASTIC_PASSWORD = getRequiredProperty(ELASTICSEARCH_PASSWORD_PROPERTY);

    public static String getAlias() {
        return String.format("brukerindeks_%s", EnvironmentUtils.requireEnvironmentName());
    }

    public static String getElasticHostname() {
        if (getEnvironmentClass() == Q) {
            return "tpa-veilarbelastic-elasticsearch.nais.preprod.local";
        } else {
            return "tpa-veilarbelastic-elasticsearch.nais.adeo.no";
        }
    }

    private static final ClientConfig DEFAULT_CONFIG = ClientConfig.builder()
            .hostname(IndekseringConfig.getElasticHostname())
            .username(VEILARBELASTIC_USERNAME)
            .password(VEILARBELASTIC_PASSWORD)
            .build();

    private static RestHighLevelClient createClient(ClientConfig config) {
        return new RestHighLevelClient(
                RestClient.builder(new HttpHost(
                        config.getHostname(),
                        config.getPort(),
                        config.getScheme())
                )
                        .setHttpClientConfigCallback(getHttpClientConfigCallback(config))
                        .setRequestConfigCallback(getRequestConfigCallback())
        );
    }

    private static RestClientBuilder.RequestConfigCallback getRequestConfigCallback() {
        return requestConfigBuilder -> requestConfigBuilder
                .setConnectTimeout(10)
                .setSocketTimeout(120_000);
    }

    private static RestClientBuilder.HttpClientConfigCallback getHttpClientConfigCallback(ClientConfig config) {

        return new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                return httpClientBuilder.setDefaultCredentialsProvider(createCredentialsProvider());
            }

            private CredentialsProvider createCredentialsProvider() {
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
                        config.getUsername(),
                        config.getPassword()
                );

                BasicCredentialsProvider provider = new BasicCredentialsProvider();
                provider.setCredentials(AuthScope.ANY, credentials);
                return provider;
            }
        };
    }

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        return createClient(DEFAULT_CONFIG);
    }

    @Bean
    public ElasticSearchHelsesjekk elasticSearchHelsesjekk() {
        return new ElasticSearchHelsesjekk(restHighLevelClient());
    }

    @Bean
    public HovedIndekseringHelsesjekk hovedIndekseringHelsesjekk(ElasticSearchService elasticSearchService) {
        return new HovedIndekseringHelsesjekk(elasticSearchService);
    }

    @Bean
    public MetricsReporter metricsReporter(UnleashService unleashService) {
        return new MetricsReporter(unleashService, restHighLevelClient());
    }

    @Value
    @Builder
    public static class ClientConfig {
        @NotEmpty
        private String username;

        @NotEmpty
        private String password;

        @NotEmpty
        private String hostname;

        @Builder.Default
        private int port = -1;

        @Builder.Default
        private String scheme = "https";
    }


    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public SolrClient solrClientSlave() {
        return new HttpSolrClient.Builder()
                .withBaseSolrUrl(URL)
                .withConnectionTimeout(2000)
                .withSocketTimeout(10000)
                .withHttpClient(createHttpClientForSolr())
                .build();
    }

    @Bean
    public SolrClient solrClientMaster() {
        return new HttpSolrClient.Builder()
                .withBaseSolrUrl(getRequiredProperty(VEILARBPORTEFOLJE_SOLR_MASTERNODE_PROPERTY))
                .withConnectionTimeout(2000)
                .withSocketTimeout(10000)
                .withHttpClient(createHttpClientForSolr())
                .build();
    }

    @Bean
    public SolrService solrService(
            SolrClient solrClientMaster,
            SolrClient solrClientSlave,
            BrukerRepository brukerRepository,
            AktoerService aktoerService,
            AktivitetDAO aktivitetDAO,
            VeilederService veilederService,
            LockingTaskExecutor lockingTaskExecutor,
            PepClient pepClient) {
        return new SolrService(solrClientMaster, solrClientSlave, brukerRepository, aktoerService, veilederService, aktivitetDAO, lockingTaskExecutor, pepClient);
    }

    @Bean
    public ElasticSearchService elasticSearchService(AktivitetDAO aktivitetDAO, BrukerRepository brukerRepository, LockingTaskExecutor shedlock) {
        return new ElasticSearchService(aktivitetDAO, brukerRepository, shedlock, restHighLevelClient());
    }

    @Bean
    public IndekseringService indekseringService(SolrService solrService, ElasticSearchService elasticSearchService, UnleashService unleashService) {
        return new IndekseringServiceProxy(solrService, elasticSearchService, unleashService);
    }


    @Bean
    public Pingable solrServerPing() {
        PingMetadata metadata = new PingMetadata(
                UUID.randomUUID().toString(),
                "HTTP via " + URL,
                "Solr-indeks for portefolje",
                true
        );
        return () -> ping(this::doPing, metadata);
    }

    @SneakyThrows
    private void doPing() {
        solrClientSlave().ping();
    }

    private HttpClient createHttpClientForSolr() {
        String username = getRequiredProperty(SYSTEMUSER_USERNAME);
        String password = getRequiredProperty(SYSTEMUSER_PASSWORD);
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
