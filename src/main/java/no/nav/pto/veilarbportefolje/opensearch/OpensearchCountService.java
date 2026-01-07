package no.nav.pto.veilarbportefolje.opensearch;


import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestUtils;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchClientConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Base64;

import static no.nav.common.rest.client.RestClient.baseClient;
import static no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS;

@Slf4j
@Service
public class OpensearchCountService {
    private final OpensearchClientConfig opensearchClientConfig;
    private final OkHttpClient client;

    @Autowired
    public OpensearchCountService(
            OpensearchClientConfig opensearchClientConfig
    ) {
        this.opensearchClientConfig = opensearchClientConfig;
        client = baseClient();
    }

    @SneakyThrows
    public long getCount() {
        String url = createAbsoluteUrl(opensearchClientConfig, BRUKERINDEKS_ALIAS) + "_count";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", getAuthHeaderValue(opensearchClientConfig))
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);

            return RestUtils.parseJsonResponse(response, CountResponse.class)
                    .map(CountResponse::getCount)
                    .orElse(0L);
        }
    }

    public static String createAbsoluteUrl(OpensearchClientConfig config, String BRUKERINDEKS_ALIAS) {
        return String.format("%s%s/",
                createAbsoluteUrl(config),
                BRUKERINDEKS_ALIAS
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