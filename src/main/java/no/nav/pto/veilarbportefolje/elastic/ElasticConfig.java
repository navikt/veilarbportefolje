package no.nav.pto.veilarbportefolje.elastic;

import no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.config.DatabaseConfig;
import no.nav.pto.veilarbportefolje.config.ServiceConfig;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import no.nav.pto.veilarbportefolje.abac.PepClient;
import no.nav.pto.veilarbportefolje.service.VeilederService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_PASSWORD_PROPERTY;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_USERNAME_PROPERTY;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.*;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@Import({
        DatabaseConfig.class,
        ServiceConfig.class
})
public class ElasticConfig {

    public static String VEILARBELASTIC_USERNAME = getRequiredProperty(ELASTICSEARCH_USERNAME_PROPERTY);
    public static String VEILARBELASTIC_PASSWORD = getRequiredProperty(ELASTICSEARCH_PASSWORD_PROPERTY);

    private static ElasticClientConfig defaultConfig = ElasticClientConfig.builder()
            .username(VEILARBELASTIC_USERNAME)
            .password(VEILARBELASTIC_PASSWORD)
            .hostname(getElasticHostname())
            .port(getElasticPort())
            .scheme(getElasticScheme())
            .build();

    private static int SOCKET_TIMEOUT = 120_000;
    private static int CONNECT_TIMEOUT = 60_000;

    @Bean
    public static RestHighLevelClient restHighLevelClient() {
        return createClient(defaultConfig);
    }

    @Bean
    public ElasticSelftest elasticSearchHelsesjekk() {
        return new ElasticSelftest(restHighLevelClient());
    }

    @Bean
    public ElasticIndexer elasticIndexer(AktivitetDAO aktivitetDAO, BrukerRepository brukerRepository, PepClient pepClient, VeilederService veilederService, UnleashService unleashService) {
        ElasticService elasticService = new ElasticService(restHighLevelClient(), pepClient, veilederService, unleashService);
        return new ElasticIndexer(aktivitetDAO, brukerRepository, restHighLevelClient(), elasticService,unleashService);
    }

    public static RestHighLevelClient createClient(ElasticClientConfig config) {
        HttpHost httpHost = new HttpHost(
                config.getHostname(),
                config.getPort(),
                config.getScheme());

        return new RestHighLevelClient(RestClient.builder(httpHost)
                .setHttpClientConfigCallback(getHttpClientConfigCallback(config))
                .setMaxRetryTimeoutMillis(SOCKET_TIMEOUT)
                .setRequestConfigCallback(
                        requestConfig -> {
                            requestConfig.setConnectTimeout(CONNECT_TIMEOUT);
                            requestConfig.setSocketTimeout(SOCKET_TIMEOUT);
                            requestConfig.setConnectionRequestTimeout(0); // http://www.github.com/elastic/elasticsearch/issues/24069
                            return requestConfig;
                        }
                ));
    }

    private static HttpClientConfigCallback getHttpClientConfigCallback(ElasticClientConfig config) {

        return new HttpClientConfigCallback() {

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
}
