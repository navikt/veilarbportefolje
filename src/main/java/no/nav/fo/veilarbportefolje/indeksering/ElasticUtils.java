package no.nav.fo.veilarbportefolje.indeksering;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.CheckedFunction;

import javax.validation.constraints.NotEmpty;

import static no.nav.fo.veilarbportefolje.indeksering.IndekseringConfig.VEILARBELASTIC_PASSWORD;
import static no.nav.fo.veilarbportefolje.indeksering.IndekseringConfig.VEILARBELASTIC_USERNAME;

public class ElasticUtils {

    public static final ClientConfig DEFAULT_CONFIG = ClientConfig.builder()
            .hostname(IndekseringConfig.getElasticHostname())
            .username(VEILARBELASTIC_USERNAME)
            .password(VEILARBELASTIC_PASSWORD)
            .build();

    public static RestHighLevelClient createClient(ClientConfig config) {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(config.getHostname(), config.getPort(), config.getScheme())
                ).setHttpClientConfigCallback(getHttpClientConfigCallback(config))
        );
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

    @SneakyThrows
    public static <T> T withLowLevelClient(CheckedFunction<RestClient, T, Exception> function) {
        T response;
        try (RestClient client = createClient(DEFAULT_CONFIG).getLowLevelClient()) {
            response = function.apply(client);
        }
        return response;
    }

    @SneakyThrows
    public static <T> T withClient(CheckedFunction<RestHighLevelClient, T, Exception> function) {

        T response;
        try (RestHighLevelClient client = createClient(DEFAULT_CONFIG)) {
            response = function.apply(client);
        }
        return response;
    }

    @SneakyThrows
    public static ClusterHealthResponse clusterHealthcheck() {
        return createClient(DEFAULT_CONFIG)
                .cluster()
                .health(new ClusterHealthRequest(), RequestOptions.DEFAULT);
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
}
