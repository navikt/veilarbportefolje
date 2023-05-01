package no.nav.pto.veilarbportefolje.vedtakstotte;

import lombok.SneakyThrows;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.UrlUtils;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.kodeverk.CacheConfig;
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakApiDto;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;

import javax.ws.rs.core.HttpHeaders;
import java.util.Optional;
import java.util.function.Supplier;

import static no.nav.common.rest.client.RestClient.*;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.rest.client.RestUtils.throwIfNotSuccessful;

public class VedtaksstotteClient {
    private final AuthService authService;
    private final String baseURL;
    private final OkHttpClient client;
    private final Supplier<String> machineToMachineTokenSupplier;
    private final EnvironmentProperties environmentProperties;

    public VedtaksstotteClient(
            String baseUrl,
            AuthService authService,
            Supplier<String> machineToMachineTokenSupplier,
            EnvironmentProperties environmentProperties
    ) {
        this.authService = authService;
        this.baseURL = baseUrl;
        this.client = baseClient();
        this.machineToMachineTokenSupplier = machineToMachineTokenSupplier;
        this.environmentProperties = environmentProperties;
    }

    @Cacheable(CacheConfig.VEDTAKSSTOTTE_PILOT_TOGGLE_CACHE_NAME)
    public boolean erVedtakstottePilotPa(EnhetId enhetId){
        return erVedtakstottePilotPaRequest(enhetId);
    }

    @SneakyThrows
    public Optional<Siste14aVedtakApiDto> hentSiste14aVedtak(Fnr fnr) {
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(baseURL, "/api/siste-14a-vedtak?fnr=" + fnr))
                .header(HttpHeaders.ACCEPT, MEDIA_TYPE_JSON.toString())
                .header("Authorization", "Bearer " + machineToMachineTokenSupplier.get())
                .build();

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponse(response, Siste14aVedtakApiDto.class);
        }
    }

    private boolean erVedtakstottePilotPaRequest(EnhetId enhetId) {
        if (enhetId == null) {
            return false;
        }
        String tokenScope = environmentProperties.getVeilarbvedtaksstotteScope();
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(baseURL, "/api/utrulling/erUtrullet?enhetId=" + enhetId.get()))
                .header(HttpHeaders.ACCEPT, MEDIA_TYPE_JSON.toString())
                .header("Authorization", "Bearer " + authService.getOboToken(tokenScope))
                .build();

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, Boolean.class);
        } catch (Exception exception) {
            return false;
        }
    }

}
