package no.nav.pto.veilarbportefolje.elastic;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticIndex;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;

import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.VEILARBELASTIC_PASSWORD;
import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.VEILARBELASTIC_USERNAME;

@Slf4j
@Service
public class ElasticCountService {

    private final ElasticClientConfig elasticsearchElasticClientConfig;
    private final String indexName;

    @Autowired
    public ElasticCountService(
            ElasticClientConfig elasticsearchElasticClientConfig,
            ElasticIndex elasticIndex
    ) {
        this.elasticsearchElasticClientConfig = elasticsearchElasticClientConfig;
        this.indexName = elasticIndex.getIndex();
    }

    @SneakyThrows
    public long getCount() {
        String url = createAbsoluteUrl(elasticsearchElasticClientConfig, indexName) + "_doc/_count";
        OkHttpClient client = no.nav.common.rest.client.RestClient.baseClient();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", getAuthHeaderValue())
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, CountResponse.class)
                    .map(CountResponse::getCount)
                    .orElse(0L);
        }
    }

    private static String createAbsoluteUrl(ElasticClientConfig config, String indexName) {
        return String.format(
                "%s://%s:%s/%s/",
                "https",
                config.getHostname(),
                config.getPort(),
                indexName
        );
    }

    private static String getAuthHeaderValue() {
        String auth = VEILARBELASTIC_USERNAME + ":" + VEILARBELASTIC_PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    @Data
    private static class CountResponse {
        private long count;
    }

}
