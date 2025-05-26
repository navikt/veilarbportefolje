package no.nav.pto.veilarbportefolje.ensligforsorger.client;

import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.HealthCheckUtils;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.OvergangsstønadResponseDto;
import okhttp3.*;

import java.util.Optional;
import java.util.function.Supplier;

import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;
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

    public Optional<OvergangsstønadResponseDto> hentEnsligForsorgerOvergangsstonad(Fnr personIdent) {
        Request request = new Request.Builder()
                .url(joinPaths(ensligForsorgerUrl, "/api/ekstern/perioder/perioder-aktivitet"))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header("Authorization", "Bearer " + machineToMachineTokenSupplier.get())
                .post(RequestBody.create(JsonUtils.toJson(new EnsligForsorgerRequestParam(personIdent.get())), MEDIA_TYPE_JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            secureLog.info("Response fra kall perioder-aktivitet: {}", response);
            return Optional.of(RestUtils.parseJsonResponseOrThrow(response, OvergangsstønadResponseDto.class));
        } catch (Exception exception) {
            secureLog.info("hentEnsligForsorgerOvergangsstonad returnerer feil med ", exception);
            return Optional.empty();
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
