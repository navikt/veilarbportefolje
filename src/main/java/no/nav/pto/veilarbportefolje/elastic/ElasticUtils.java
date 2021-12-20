package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.xcontent.XContentType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.BRUKERINDEKS_ALIAS;
import static org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions.Type.ADD;
import static org.elasticsearch.client.RequestOptions.DEFAULT;

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
    public static void opprettNyIndeks(RestHighLevelClient restHighLevelClient) {
        Path elasticSettingsPath = Paths.get("src", "test", "resources", "elastic_settings.json");
        String json = Files.readString(elasticSettingsPath).trim();
        String navn = createIndexName();

        CreateIndexRequest request = new CreateIndexRequest(navn)
                .source(json, XContentType.JSON);

        CreateIndexResponse response = restHighLevelClient.indices().create(request, DEFAULT);

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke opprette ny indeks {}", navn);
            throw new RuntimeException();
        }

        //do: hovedindeksering

        opprettAliasForIndeks(navn, restHighLevelClient);
    }

    public static String createIndexName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        String timestamp = LocalDateTime.now().format(formatter);
        return String.format("%s_%s", BRUKERINDEKS_ALIAS, timestamp);
    }

    @SneakyThrows
    private static void opprettAliasForIndeks(String indeks, RestHighLevelClient restHighLevelClient) {
        IndicesAliasesRequest.AliasActions addAliasAction = new IndicesAliasesRequest.AliasActions(ADD)
                .index(indeks)
                .alias(BRUKERINDEKS_ALIAS);

        IndicesAliasesRequest request = new IndicesAliasesRequest().addAliasAction(addAliasAction);
        AcknowledgedResponse response = restHighLevelClient.indices().updateAliases(request, DEFAULT);

        if (!response.isAcknowledged()) {
            log.error("Kunne ikke legge til alias {}", BRUKERINDEKS_ALIAS);
            throw new RuntimeException();
        }
    }

}
