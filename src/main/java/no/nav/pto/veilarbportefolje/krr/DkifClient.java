package no.nav.pto.veilarbportefolje.krr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Optional;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class DkifClient {

    private final String dkifUrl;

    private final OkHttpClient client;

    public DkifClient(String dkifUrl, OkHttpClient client) {
        this.dkifUrl = dkifUrl;
        this.client = client;
    }

    @SneakyThrows
    public DkifKontaktinfo hentKontaktInfo(String fnr) {
        Request request = new Request.Builder()
                .url(joinPaths(dkifUrl, "/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false"))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header("Nav-Personidenter", fnr)
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            Optional<String> json = RestUtils.getBodyStr(response);

            if (json.isEmpty()) {
                DkifKontaktinfo dkifKontaktinfo = new DkifKontaktinfo();
                dkifKontaktinfo.setPersonident(fnr);
                return dkifKontaktinfo;
            }

            ObjectMapper mapper = JsonUtils.getMapper();
            JsonNode node = mapper.readTree(json.get());

            return mapper.treeToValue(node.get("kontaktinfo").get(fnr), DkifKontaktinfo.class);
        }
    }

    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(dkifUrl, "/internal/isAlive"), client);
    }

}