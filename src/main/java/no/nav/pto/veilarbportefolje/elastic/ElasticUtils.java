package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.pto.veilarbportefolje.elastic.domene.CountResponse;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.Base64;

import static no.nav.common.utils.EnvironmentUtils.resolveHostName;
import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.VEILARBELASTIC_PASSWORD;
import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.VEILARBELASTIC_USERNAME;

@Slf4j
public class ElasticUtils {

    public static final String NAIS_LOADBALANCED_HOSTNAME = "tpa-veilarbelastic-elasticsearch.nais.preprod.local";
    public static final String NAIS_INTERNAL_CLUSTER_HOSTNAME = "tpa-veilarbelastic-elasticsearch.tpa.svc.nais.local";

    private static int SOCKET_TIMEOUT = 120_000;
    private static int CONNECT_TIMEOUT = 60_000;

    public static RestHighLevelClient createClient(ElasticClientConfig config) {
        HttpHost httpHost = new HttpHost(
                config.getHostname(),
                config.getPort(),
                config.getScheme()
        );

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

    private static RestClientBuilder.HttpClientConfigCallback getHttpClientConfigCallback(ElasticClientConfig config) {
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

    @SneakyThrows
    public static long getCount() {
        log.info("get count 1");
        String url = ElasticUtils.getAbsoluteUrl() + "_doc/_count";
        OkHttpClient client = no.nav.common.rest.client.RestClient.baseClient();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", getAuthHeaderValue())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            log.info("get count 2");
            log.info(response.toString());
           return RestUtils.parseJsonResponse(response, CountResponse.class)
                   .map(CountResponse::getCount)
                   .orElse(0L);
        }
    }

    static String getAbsoluteUrl() {
        return String.format(
                "%s://%s:%s/%s/",
                getElasticScheme(),
                getElasticHostname(),
                getElasticPort(),
                getAlias()
        );
    }

    static String getAuthHeaderValue() {
        String auth = VEILARBELASTIC_USERNAME + ":" + VEILARBELASTIC_PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    public static String getAlias() {
        return "brukerindeks";
    }

    static String getElasticScheme() {
        if (onDevillo()) {
            return "https";
        } else {
            return "http";
        }
    }

    static int getElasticPort() {
        if (onDevillo()) {
            return 443;
        } else {
            return 9200;
        }
    }

    static String getElasticHostname() {
        if (onDevillo()) {
            return NAIS_LOADBALANCED_HOSTNAME;
        } else {
            return NAIS_INTERNAL_CLUSTER_HOSTNAME;
        }
    }

    public static boolean onDevillo() {
        String hostname = resolveHostName();
        return hostname.contains("devillo.no");
    }
}
