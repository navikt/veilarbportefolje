package no.nav.pto.veilarbportefolje.kodeverk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class KodeverkClientImpl implements KodeverkClient {

    private final String kodeverkUrl;

    private final OkHttpClient client;

    public KodeverkClientImpl(String kodeverkUrl) {
        this.kodeverkUrl = kodeverkUrl;
        this.client = RestClient.baseClient();
    }

    @Cacheable(CacheConfig.KODEVERK_BETYDNING_CACHE_NAME)
    @SneakyThrows
    public Map<String, String> hentKodeverkBeskrivelser(String kodeverksnavn) {
        Request request = new Request.Builder()
                .url(joinPaths(kodeverkUrl, format("/api/v1/kodeverk/%s/koder/betydninger?ekskluderUgyldige=false&spraak=nb", kodeverksnavn)))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            Optional<String> maybeJsonResponse = RestUtils.getBodyStr(response);

            if (maybeJsonResponse.isEmpty()) {
                throw new IllegalStateException("JSON is missing from body");
            }

            return parseKodeverkBetydningJson(maybeJsonResponse.get());
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        return HealthCheckUtils.pingUrl(joinPaths(kodeverkUrl, "/internal/isAlive"), client);
    }

    @SneakyThrows
    private Map<String, String> parseKodeverkBetydningJson(String responseJson) {
        Map<String, String> betydningerMap = new HashMap<>();

        JsonNode rootNode = JsonUtils.getMapper().readTree(responseJson);
        JsonNode betydninger = rootNode.get("betydninger");

        betydninger.fieldNames().forEachRemaining((betydningName) -> {
            JsonNode betydningerValues = betydninger.get(betydningName);
            AtomicReference<JsonNode> betydningNyeste = new AtomicReference<>(betydninger.get(betydningName).get(0));

            //find most recent value
            if (betydningerValues.isArray()) {
                ArrayNode arrayField = (ArrayNode) betydningerValues;
                arrayField.forEach(node -> {
                    Timestamp gyldigFra = DateUtils.getTimestampFromSimpleISODate(node.get("gyldigFra").asText());
                    Timestamp gyldigFraNyeste = DateUtils.getTimestampFromSimpleISODate(betydningNyeste.get().get("gyldigFra").asText());
                    if (gyldigFra != null && gyldigFraNyeste != null && gyldigFra.after(gyldigFraNyeste)) {
                        betydningNyeste.set(node);
                    }
                });
            }

            // Noen koder mangler informasjon
            if (betydningNyeste.get() == null) {
                return;
            }

            JsonNode betydningBeskrivelserNode = betydningNyeste.get().get("beskrivelser");
            JsonNode beskrivelseNbNode = betydningBeskrivelserNode.get("nb");
            String beskrivelseNb = beskrivelseNbNode.get("tekst").asText();

            betydningerMap.put(betydningName, beskrivelseNb);
        });

        return betydningerMap;
    }
}
