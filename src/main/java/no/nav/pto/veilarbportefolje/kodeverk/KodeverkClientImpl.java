package no.nav.pto.veilarbportefolje.kodeverk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static java.lang.String.format;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
public class KodeverkClientImpl implements KodeverkClient {

    private final String kodeverkUrl;

    private final Supplier<String> machineToMachineTokenSupplier;

    private final OkHttpClient client;

    public KodeverkClientImpl(String kodeverkUrl, Supplier<String> machineToMachineTokenSupplier) {
        this.kodeverkUrl = kodeverkUrl;
        this.machineToMachineTokenSupplier = machineToMachineTokenSupplier;
        this.client = RestClient.baseClient();
    }

    @Cacheable(CacheConfig.KODEVERK_BETYDNING_CACHE_NAME)
    @SneakyThrows
    public Map<String, String> hentKodeverkBeskrivelser(String kodeverksnavn) {
        Request request = new Request.Builder()
                .url(joinPaths(kodeverkUrl, format("/api/v1/kodeverk/%s/koder/betydninger?ekskluderUgyldige=false&spraak=nb", kodeverksnavn)))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + machineToMachineTokenSupplier.get())
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
        Request request = new Request.Builder()
                .url(joinPaths(kodeverkUrl, "/rest/ping"))
                .header(AUTHORIZATION, "Bearer " + machineToMachineTokenSupplier.get())
                .build();

        return HealthCheckUtils.pingUrl(request, client);
    }

    private Map<String, String> parseKodeverkBetydningJson(String responseJson) {
        Map<String, String> betydningerMap = new HashMap<>();
        try {
            JsonNode rootNode = JsonUtils.getMapper().readTree(responseJson);
            JsonNode betydninger = rootNode.get("betydninger");

            betydninger.fieldNames().forEachRemaining((betydningName) -> {
                JsonNode betydningerValues = betydninger.get(betydningName);
                JsonNode betydningNyeste;

                //find most recent value
                if (betydningerValues.isArray()) {
                    ArrayNode arrayField = (ArrayNode) betydningerValues;
                    betydningNyeste = StreamSupport
                            .stream(arrayField.spliterator(), false)
                            .filter(betydning -> betydning.get("gyldigFra") != null)
                            .max(Comparator.comparing(o -> Integer.valueOf(o.get("gyldigFra").asText().substring(0, 4))))
                            .orElse(null);
                } else {
                    betydningNyeste = betydningerValues;
                }

                // Noen koder mangler informasjon
                if (betydningNyeste == null) {
                    return;
                }

                JsonNode betydningBeskrivelserNode = betydningNyeste.get("beskrivelser");
                JsonNode beskrivelseNbNode = betydningBeskrivelserNode.get("nb");
                String beskrivelseNb = beskrivelseNbNode.get("tekst").asText();

                if (beskrivelseNb.isEmpty()) {
                    beskrivelseNb = beskrivelseNbNode.get("term").asText();
                }

                betydningerMap.put(betydningName, beskrivelseNb);
            });

            return betydningerMap;
        } catch (Exception e) {
            log.warn("Can't parse kodeverk values " + e, e);
            return betydningerMap;
        }
    }
}
