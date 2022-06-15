package no.nav.pto.veilarbportefolje.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.common.utils.UrlUtils;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.auth.DownstreamApi;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.ws.rs.core.HttpHeaders;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.rest.client.RestUtils.throwIfNotSuccessful;

public class VedtakstottePilotRequest {
    private final DownstreamApi veilarbVedtakstotteApi;
    private final Function<DownstreamApi, Optional<String>> aadOboTokenProvider;
    private final String baseURL;
    private final OkHttpClient client;
    private final Cache<EnhetId, Boolean > hentVedtakstotteCache;

    public VedtakstottePilotRequest(AuthService authService) {
        this.veilarbVedtakstotteApi = new DownstreamApi(EnvironmentUtils.requireClusterName(), "pto", "veilarbvedtaksstotte");
        this.baseURL = UrlUtils.createServiceUrl("veilarbvedtaksstotte", "pto", true);
        this.aadOboTokenProvider =  authService::getAadOboTokenForTjeneste;
        this.client = no.nav.common.rest.client.RestClient.baseClient();

        this.hentVedtakstotteCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }

    public boolean erVedtakstottePilotPa(EnhetId enhetId){
        return tryCacheFirst(hentVedtakstotteCache, enhetId, () -> this.erVedtakstottePilotPaRequest(enhetId));
    }

    private boolean erVedtakstottePilotPaRequest(EnhetId enhetId) {
        if (enhetId == null) {
            return false;
        }
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(baseURL, "/api/utrulling/erUtrullet?enhetId=" + enhetId.get()))
                .header(HttpHeaders.ACCEPT, MEDIA_TYPE_JSON.toString())
                .header("Authorization", "Bearer " + aadOboTokenProvider.apply(veilarbVedtakstotteApi)
                        .orElseGet(AuthUtils::getInnloggetBrukerToken))
                .build();

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, Boolean.class);
        } catch (Exception exception) {
            return false;
        }
    }
}
