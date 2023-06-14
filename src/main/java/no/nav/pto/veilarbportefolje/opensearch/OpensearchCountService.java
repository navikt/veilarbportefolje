package no.nav.pto.veilarbportefolje.opensearch;


import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchClientConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;

import static no.nav.common.rest.client.RestClient.baseClient;

@Slf4j
@Service
public class OpensearchCountService {
    private final OpensearchClientConfig opensearchClientConfig;
    private final String indexName;
    private final MetricsClient metricsClient;
    private final OkHttpClient client;

    @Autowired
    public OpensearchCountService(
            OpensearchClientConfig opensearchClientConfig,
            IndexName opensearchIndex,
            MetricsClient metricsClient
    ) {
        this.opensearchClientConfig = opensearchClientConfig;
        this.metricsClient = metricsClient;
        this.indexName = opensearchIndex.getValue();
        client = baseClient();
    }

    @SneakyThrows
    public long getCount() {
        String url = createAbsoluteUrl(opensearchClientConfig, indexName) + "/_count";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", getAuthHeaderValue(opensearchClientConfig))
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

    public static String createAbsoluteUrl(OpensearchClientConfig config, String indexName) {
        return String.format("%s%s/",
                createAbsoluteUrl(config),
                indexName
        );
    }

    public static String createAbsoluteUrl(OpensearchClientConfig config) {
        return String.format(
                "%s://%s:%s/",
                config.getScheme(),
                config.getHostname(),
                config.getPort()
        );
    }

    public static String getAuthHeaderValue(OpensearchClientConfig config) {
        String auth = config.getUsername() + ":" + config.getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    @Data
    private static class CountResponse {
        private long count;
    }

}