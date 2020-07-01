package no.nav.pto.veilarbportefolje.client;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.auth.subject.SsoToken;
import no.nav.common.auth.subject.SubjectHandler;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;

import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static no.nav.pto.veilarbportefolje.config.CacheConfig.VEILARBVEILEDER;

@Slf4j
public class VeilarbVeilederClient {
    private final static String BEARER = "Bearer ";
    private final static String SEPARATOR = " ";

    private final String url;
    private final OkHttpClient client = RestClient.baseClient();

    public VeilarbVeilederClient(String namespace, OkHttpClient client) {
        url = format("http://veilarbveileder.%s.svc.nais.local/veilarbveileder", namespace);
    }

    @Cacheable(VEILARBVEILEDER)
    @SneakyThrows
    public List<VeilederId> hentVeilederePaaEnhet(String enhet) {
        String path = format("/api/enhet/%s/identer", enhet);

        String ssoToken = SubjectHandler.getSsoToken(SsoToken.Type.OIDC).orElseThrow(IllegalStateException::new);

        Request request  = new Request.Builder()
                .url(url + path)
                .header(AUTHORIZATION, BEARER + SEPARATOR + ssoToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseArrayOrThrow(response, VeilederId.class);
        }
    }
}
