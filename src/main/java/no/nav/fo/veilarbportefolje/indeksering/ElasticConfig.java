package no.nav.fo.veilarbportefolje.indeksering;

import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.config.DatabaseConfig;
import no.nav.fo.veilarbportefolje.config.ServiceConfig;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.indeksering.domene.ElasticClientConfig;
import no.nav.fo.veilarbportefolje.service.PepClient;
import no.nav.fo.veilarbportefolje.service.VeilederService;
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

import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_PASSWORD_PROPERTY;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_USERNAME_PROPERTY;
import static no.nav.fo.veilarbportefolje.indeksering.ElasticUtils.*;
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

    private static ElasticClientConfig defaultConfig = ElasticClientConfig.builder()
            .username(VEILARBELASTIC_USERNAME)
            .password(VEILARBELASTIC_PASSWORD)
            .hostname(getElasticHostname())
            .port(getElasticPort())
            .scheme(getElasticScheme())
            .build();

    @Bean
    public static RestHighLevelClient restHighLevelClient() {
        return createClient(defaultConfig);
    }

    @Bean
    public ElasticSelftest elasticSearchHelsesjekk() {
        return new ElasticSelftest(restHighLevelClient());
    }

    @Bean
    public ElasticIndexer elasticSearchService(AktivitetDAO aktivitetDAO, BrukerRepository brukerRepository, LockingTaskExecutor shedlock, PepClient pepClient, VeilederService veilederService) {
        ElasticService elasticService = new ElasticService(restHighLevelClient(), pepClient, veilederService);
        return new ElasticIndexer(aktivitetDAO, brukerRepository, restHighLevelClient(), elasticService);
    }

    public static RestHighLevelClient createClient(ElasticClientConfig config) {
        HttpHost httpHost = new HttpHost(
                config.getHostname(),
                config.getPort(),
                config.getScheme());

        return new RestHighLevelClient(RestClient.builder(httpHost)
                .setHttpClientConfigCallback(getHttpClientConfigCallback(config))
                .setRequestConfigCallback(
                        requestConfig -> {
                            requestConfig.setConnectTimeout(10);
                            requestConfig.setSocketTimeout(120000);
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
