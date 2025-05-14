package no.nav.pto.veilarbportefolje.ensligforsorger.client;

import lombok.SneakyThrows;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.EnsligForsorgerResponseDto;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.Optional;
import java.util.function.Supplier;

import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class EnsligForsorgerClientImpl implements EnsligForsorgerClient {

    private final String ensligForsorgerUrl;

    private final Supplier<String> machineToMachineTokenSupplier;

    private final OkHttpClient client;

    public EnsligForsorgerClientImpl(String ensligForsorgerUrl, Supplier<String> machineToMachineTokenSupplier) {
        this.ensligForsorgerUrl = ensligForsorgerUrl;
        this.machineToMachineTokenSupplier = machineToMachineTokenSupplier;
        this.client = RestClient.baseClient();
    }
    @SneakyThrows
    public Optional<EnsligForsorgerResponseDto> hentEnsligForsorgerOvergangsstonad(String personIdent) {
        Request request = new Request.Builder()
                .url(joinPaths(ensligForsorgerUrl, "/api/v1/ensligforsorger/"))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + machineToMachineTokenSupplier.get())
                .post(RequestBody.create(JsonUtils.toJson(new Fnr(personIdent)), MEDIA_TYPE_JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, EnsligForsorgerResponseDto.class);
        }
    }

    @Override
    public HealthCheckResult checkHealth() {
        Request request = new Request.Builder()
                .url(joinPaths(ensligForsorgerUrl, "rest/ping"))
                .header(AUTHORIZATION, "Bearer " + machineToMachineTokenSupplier.get())
                .build();

        return HealthCheckUtils.pingUrl(request, client);
    }
}
