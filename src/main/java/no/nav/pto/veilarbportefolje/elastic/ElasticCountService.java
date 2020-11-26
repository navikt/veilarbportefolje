package no.nav.pto.veilarbportefolje.elastic;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.rest.client.RestUtils;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticIndex;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Base64;

import static no.nav.pto.veilarbportefolje.elastic.ElasticConfig.*;

@Slf4j
@Service
public class ElasticCountService {

    private final UnleashService unleashService;
    private final ElasticClientConfig naisTpaElasticClientConfig;
    private final ElasticClientConfig ptoElasticsearchElasticClientConfig;
    private final String indexName;

    @Autowired
    public ElasticCountService(
            UnleashService unleashService,
            @Qualifier(NAIS_TPA_CLIENT_CONFIG) ElasticClientConfig naisTpaElasticClientConfig,
            @Qualifier(PTO_ELASTICSEARCH_CLIENT_CONFIG) ElasticClientConfig ptoElasticsearchElasticClientConfig,
            ElasticIndex elasticIndex
    ) {
        this.unleashService = unleashService;
        this.naisTpaElasticClientConfig = naisTpaElasticClientConfig;
        this.ptoElasticsearchElasticClientConfig = ptoElasticsearchElasticClientConfig;
        this.indexName = elasticIndex.getIndex();
    }

    @SneakyThrows
    public long getCount() {
        ElasticClientConfig config = unleashService.isEnabled(USE_PTO_ELASTICSEARCH_TOGGLE)
                ? ptoElasticsearchElasticClientConfig
                : naisTpaElasticClientConfig;

        String url = createAbsoluteUrl(config, indexName) + "_doc/_count";
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
