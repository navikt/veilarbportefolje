package no.nav.pto.veilarbportefolje.elastic;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.test.ssl.SSLTestUtils;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class ElasticUtils {

    private static final int SOCKET_TIMEOUT = 120_000;
    private static final int CONNECT_TIMEOUT = 60_000;

    public static RestHighLevelClient createClient(ElasticClientConfig config) {
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

    private static RestClientBuilder.HttpClientConfigCallback getHttpClientConfigCallback(ElasticClientConfig config) {
        if (config.isDisableSecurity()) {
            return httpClientBuilder -> httpClientBuilder;
        }

        return new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                // TODO: Siden vi bruker self-signed SSL certs så må vi skru av validering for elastic rest klienten
                //  Når vi går over til Aiven så vil vi ikke lenger trenge å skru av validering av SSL certs
                return httpClientBuilder
                        .setDefaultCredentialsProvider(createCredentialsProvider())
                        .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE) // NB!: Vil fjerne sjekk på hostname for certs
                        .setSSLContext(SSLTestUtils.sslContext); // NB!: Dette vil fjerne sjekk på SSL certs
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

    public static String createIndexName(String alias) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        String timestamp = LocalDateTime.now().format(formatter);
        return String.format("%s_%s", alias, timestamp);
    }

}
