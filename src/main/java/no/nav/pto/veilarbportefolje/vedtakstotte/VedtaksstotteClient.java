package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.UrlUtils;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.kodeverk.CacheConfig;
import no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.siste14aVedtak.Siste14aVedtakApiDto;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;

import jakarta.ws.rs.core.HttpHeaders;
import java.util.Optional;
import java.util.function.Supplier;

import static no.nav.common.rest.client.RestClient.*;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.rest.client.RestUtils.throwIfNotSuccessful;

public class VedtaksstotteClient {
    private final String baseURL;
    private final OkHttpClient client;
    private final Supplier<String> machineToMachineTokenSupplier;

    public VedtaksstotteClient(
            String baseUrl,
            Supplier<String> machineToMachineTokenSupplier
    ) {
        this.baseURL = baseUrl;
        this.client = baseClient();
        this.machineToMachineTokenSupplier = machineToMachineTokenSupplier;
    }

    @SneakyThrows
    public Optional<Siste14aVedtakApiDto> hentSiste14aVedtak(Fnr fnr) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(baseURL, "/api/v2/hent-siste-14a-vedtak"))
                .header(HttpHeaders.ACCEPT, MEDIA_TYPE_JSON.toString())
                .header("Authorization", "Bearer " + machineToMachineTokenSupplier.get())
                .post(RequestBody.create(JsonUtils.toJson(new Siste14aVedtakRequest(fnr)), MEDIA_TYPE_JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, Siste14aVedtakApiDto.class);
        }
    }

}
