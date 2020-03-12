package no.nav.pto.veilarbportefolje.elastic;

import no.nav.pto.veilarbportefolje.abac.PepClient;
import no.nav.pto.veilarbportefolje.config.DatabaseConfig;
import no.nav.pto.veilarbportefolje.config.ServiceConfig;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDAO;
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

import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.*;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@Import({
        DatabaseConfig.class,
        ServiceConfig.class
})
public class ElasticConfig {

    static int BATCH_SIZE = 1000;
    static int BATCH_SIZE_LIMIT = 1000;

    public static String VEILARBELASTIC_USERNAME = getRequiredProperty(ELASTICSEARCH_USERNAME_PROPERTY);
    public static String VEILARBELASTIC_PASSWORD = getRequiredProperty(ELASTICSEARCH_PASSWORD_PROPERTY);
    public static String VEILARBELASTIC_HOSTNAME = getRequiredProperty(VEILARBELASTIC_HOSTNAME_PROPERTY);

    private static ElasticClientConfig defaultConfig = ElasticClientConfig.builder()
            .username(VEILARBELASTIC_USERNAME)
            .password(VEILARBELASTIC_PASSWORD)
            .hostname(VEILARBELASTIC_HOSTNAME)
            .port(9200)
            .scheme("http")
            .build();

    public static String VEILARB_OPENDISTRO_ELASTICSEARCH_USERNAME = getRequiredProperty(VEILARB_OPENDISTRO_ELASTICSEARCH_USERNAME_PROPERTY);
    public static String VEILARB_OPENDISTRO_ELASTICSEARCH_PASSWORD = getRequiredProperty(VEILARB_OPENDISTRO_ELASTICSEARCH_PASSWORD_PROPERTY);
    public static String VEILARB_OPENDISTRO_ELASTICSEARCH_HOSTNAME = getRequiredProperty(VEILARB_OPENDISTRO_ELASTICSEARCH_HOSTNAME_PROPERTY);

    private static ElasticClientConfig openDistroClientConfig = ElasticClientConfig.builder()
            .username(VEILARB_OPENDISTRO_ELASTICSEARCH_USERNAME)
            .password(VEILARB_OPENDISTRO_ELASTICSEARCH_PASSWORD)
            .hostname(VEILARB_OPENDISTRO_ELASTICSEARCH_HOSTNAME)
            .port(9200)
            .scheme("http")
            .build();

    private static int SOCKET_TIMEOUT = 120_000;
    private static int CONNECT_TIMEOUT = 60_000;

    @Bean
    public static RestHighLevelClient deprecatedClient() {
        return createClient(defaultConfig);
    }

    @Bean
    public static OpenDistroClient openDistroClient() {
        return createOpenDistroClient(openDistroClientConfig);
    }

    public static OpenDistroClient createOpenDistroClient(ElasticClientConfig config) {

        HttpHost httpHost = new HttpHost(
                config.getHostname(),
                config.getPort(),
                config.getScheme());

        return new OpenDistroClient(RestClient.builder(httpHost)
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


    @Bean
    public ElasticSelftest elasticSearchHelsesjekk() {
        return new ElasticSelftest(deprecatedClient());
    }

    @Bean
    public OpenDistroElasticSelftest openDistroElasticSelftest() {
        return new OpenDistroElasticSelftest(openDistroClient());
    }

    @Bean
    public ElasticIndexer elasticIndexer(AktivitetDAO aktivitetDAO, BrukerRepository brukerRepository, PepClient pepClient, VeilederService veilederService, UnleashService unleashService) {
        ElasticService elasticService = new ElasticService(deprecatedClient(), openDistroClient(), pepClient, veilederService, unleashService);
        return new ElasticIndexer(aktivitetDAO, brukerRepository, deprecatedClient(), openDistroClient(), elasticService, unleashService);
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
