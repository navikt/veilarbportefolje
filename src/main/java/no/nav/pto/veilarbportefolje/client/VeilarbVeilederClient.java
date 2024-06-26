package no.nav.pto.veilarbportefolje.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
public class VeilarbVeilederClient {
    private final String url;
    private final OkHttpClient client;
    private final AuthService authService;
    private final Cache<EnhetId, List<String>> hentVeilederePaaEnhetCache;
    private final Cache<EnhetId, List<String>> hentVeilederePaaEnhetMachineToMachineCache;
    private final EnvironmentProperties environmentProperties;

    public VeilarbVeilederClient(AuthService authService, EnvironmentProperties environmentProperties) {
        this.authService = authService;
        this.url = environmentProperties.getVeilarbveilederUrl();
        this.client = RestClient.baseClient();
        this.environmentProperties = environmentProperties;

        hentVeilederePaaEnhetCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(600)
                .build();

        hentVeilederePaaEnhetMachineToMachineCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(600)
                .build();
    }

    public List<String> hentVeilederePaaEnhet(EnhetId enhet) {
        return tryCacheFirst(hentVeilederePaaEnhetCache, enhet,
                () -> hentVeilederePaaEnhetQuery(enhet));
    }

    public List<String> hentVeilederePaaEnhetMachineToMachine(EnhetId enhet) {
        return tryCacheFirst(hentVeilederePaaEnhetMachineToMachineCache, enhet,
                () -> hentVeilederePaaEnhetQueryMachineToMachine(enhet));
    }

    @SneakyThrows
    private List<String> hentVeilederePaaEnhetQuery(EnhetId enhet) {
        String path = format("/api/enhet/%s/identer", enhet);
        String tokenScope = environmentProperties.getVeilarbveilederScope();
        Request request = new Request.Builder()
                .header(AUTHORIZATION, "Bearer " + authService.getOboToken(tokenScope))
                .url(url + path)
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseArrayOrThrow(response, String.class);
        }
    }

    @SneakyThrows
    private List<String> hentVeilederePaaEnhetQueryMachineToMachine(EnhetId enhet) {
        String path = format("/api/enhet/%s/identer", enhet);
        String tokenScope = environmentProperties.getVeilarbveilederScope();
        Request request = new Request.Builder()
                .header(AUTHORIZATION, "Bearer " + authService.getM2MToken(tokenScope))
                .url(url + path)
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseArrayOrThrow(response, String.class);
        }
    }

}
