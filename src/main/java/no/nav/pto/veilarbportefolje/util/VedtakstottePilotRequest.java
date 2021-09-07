package no.nav.pto.veilarbportefolje.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.utils.UrlUtils;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.HttpHeaders;
import java.util.concurrent.TimeUnit;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.rest.client.RestUtils.throwIfNotSuccessful;

public class VedtakstottePilotRequest {
    private final String baseURL;
    private final OkHttpClient client;


    private final Cache<EnhetId, Boolean > hentVedtakstotteCache;

    @Autowired
    public VedtakstottePilotRequest() {
        this.client = no.nav.common.rest.client.RestClient.baseClient();
        this.baseURL = UrlUtils.createServiceUrl("veilarbvedtaksstotte", "pto", true);

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
                .header("Authorization", "Bearer " + AuthUtils.getInnloggetBrukerToken())
                .build();

        try (Response response = client.newCall(request).execute()) {
            throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, Boolean.class);
        } catch (Exception exception) {
            return false;
        }
    }
}
