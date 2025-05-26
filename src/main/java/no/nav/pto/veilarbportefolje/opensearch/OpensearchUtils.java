package no.nav.pto.veilarbportefolje.opensearch;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchClientConfig;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;

import java.util.concurrent.TimeUnit;

@Slf4j
public class OpensearchUtils {

    private static final int SOCKET_TIMEOUT = 120_000;
    private static final int CONNECT_TIMEOUT = 60_000;

    public static RestHighLevelClient createClient(OpensearchClientConfig config) {
        HttpHost httpHost = new HttpHost(
                config.getScheme(),
                config.getHostname(),
                config.getPort()
        );

        return new RestHighLevelClient(RestClient.builder(httpHost)
                .setHttpClientConfigCallback(getHttpClientConfigCallback(config))
                .setRequestConfigCallback(
                        requestConfig -> {
                            requestConfig.setConnectionRequestTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
                            requestConfig.setResponseTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS);
                            requestConfig.setConnectionRequestTimeout(Timeout.ZERO_MILLISECONDS); // http://www.github.com/elastic/elasticsearch/issues/24069
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
                        config.getPassword().toCharArray()
                );

                BasicCredentialsProvider provider = new BasicCredentialsProvider();
                provider.setCredentials(new AuthScope(null, -1), credentials);
                return provider;
            }
        };
    }

}
