package no.nav.pto.veilarbportefolje.elastic;


import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
public class ElasticCountService {
    private final ElasticClientConfig elasticsearchElasticClientConfig;
    private final String indexName;
    private final MetricsClient metricsClient;
    private final EnvironmentProperties environmentProperties;

    @Autowired
    public ElasticCountService(
            ElasticClientConfig elasticsearchElasticClientConfig,
            EnvironmentProperties environmentProperties,
            IndexName elasticIndex,
            MetricsClient metricsClient
    ) {
        this.elasticsearchElasticClientConfig = elasticsearchElasticClientConfig;
        this.environmentProperties = environmentProperties;
        this.metricsClient = metricsClient;
        this.indexName = elasticIndex.getValue();
    }

    @SneakyThrows
    public long getCount() {
        String url = createAbsoluteUrl(elasticsearchElasticClientConfig, indexName) + "_doc/_count";
        OkHttpClient client = no.nav.common.rest.client.RestClient.baseClient();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", getAuthHeaderValue(environmentProperties))
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            long count = RestUtils.parseJsonResponse(response, CountResponse.class)
                    .map(CountResponse::getCount)
                    .orElse(0L);

            reportDocCountToInfluxdb(count);
            return count;
        }
    }

    private void reportDocCountToInfluxdb(long count) {
        Event event = new Event("portefolje.antall.brukere");
        event.addFieldToReport("antall_brukere", count);

        metricsClient.report(event);
    }

    private static String createAbsoluteUrl(ElasticClientConfig config, String indexName) {
        return String.format(
                "%s://%s:%s/%s/",
                config.getScheme(),
                config.getHostname(),
                config.getPort(),
                indexName
        );
    }

    private static String getAuthHeaderValue(EnvironmentProperties environmentProperties) {
        String auth = environmentProperties.getElasticUsername() + ":" + environmentProperties.getElasticPassword();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    @Data
    private static class CountResponse {
        private long count;
    }

}