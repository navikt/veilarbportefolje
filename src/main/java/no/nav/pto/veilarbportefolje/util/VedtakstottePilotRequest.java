package no.nav.pto.veilarbportefolje.util;

import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.utils.UrlUtils;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.HttpHeaders;

import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.rest.client.RestUtils.throwIfNotSuccessful;
import static no.nav.common.utils.EnvironmentUtils.isProduction;

public class VedtakstottePilotRequest {
    private final String baseURL;
    private final OkHttpClient client;

    @Autowired
    public VedtakstottePilotRequest() {
        this.client = no.nav.common.rest.client.RestClient.baseClient();
        this.baseURL = (isProduction().orElse(false)) ? "https://app.adeo.no/" : "https://app-q1.dev.adeo.no/";
    }

    public boolean erVedtakstottePilotPa(EnhetId enhetId) {
        if (enhetId == null) {
            return false;
        }
        Request request = new Request.Builder()
                .url(UrlUtils.joinPaths(baseURL, "/veilarbvedtaksstotte/api/utrulling/" + enhetId.get()))
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
