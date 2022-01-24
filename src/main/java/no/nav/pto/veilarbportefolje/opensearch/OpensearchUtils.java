package no.nav.pto.veilarbportefolje.opensearch;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchClientConfig;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;

@Slf4j
public class OpensearchUtils {

    private static final int SOCKET_TIMEOUT = 120_000;
    private static final int CONNECT_TIMEOUT = 60_000;

    public static RestHighLevelClient createClient(OpensearchClientConfig config) {
        HttpHost httpHost = new HttpHost(
                config.getHostname(),
                config.getPort(),
                config.getScheme()
        );

        return new RestHighLevelClient(RestClient.builder(httpHost)
                .setHttpClientConfigCallback(getHttpClientConfigCallback(config))
                .setRequestConfigCallback(
                        requestConfig -> {
                            requestConfig.setConnectTimeout(CONNECT_TIMEOUT);
                            requestConfig.setSocketTimeout(SOCKET_TIMEOUT);
                            requestConfig.setConnectionRequestTimeout(0); // http://www.github.com/elastic/elasticsearch/issues/24069
                            return requestConfig;
                        }
                ));
    }

    private static RestClientBuilder.HttpClientConfigCallback getHttpClientConfigCallback(OpensearchClientConfig config) {
        if (config.isDisableSecurity()) {
            return httpClientBuilder -> httpClientBuilder;
        }

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

}
