package no.nav.pto.veilarbportefolje.client;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.utils.EnvironmentUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;
import static java.lang.String.format;
import static no.nav.common.utils.EnvironmentUtils.requireNamespace;
import static no.nav.pto.veilarbportefolje.client.RestClientUtils.authHeaderMedSystemBruker;
import static no.nav.pto.veilarbportefolje.config.CacheConfig.VEILARBVEILEDER;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
public class VeilarbVeilederClient {

    private final String url;
    private final OkHttpClient client;

    public VeilarbVeilederClient() {
        url = EnvironmentUtils.isProduction().orElseThrow()
                ? "https://veilarbveileder.nais.adeo.no/veilarbveileder"
                : format("https://veilarbveileder-%s.nais.preprod.local/veilarbveileder", requireNamespace());

        this.client = RestClient.baseClient();
    }

    @Cacheable(VEILARBVEILEDER)
    @SneakyThrows
    public List<String> hentVeilederePaaEnhet(String enhet) {
        String path = format("/api/enhet/%s/identer", enhet);


        Request request  = new Request.Builder()
                .header(AUTHORIZATION, authHeaderMedSystemBruker())
                .url(url + path)
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseArrayOrThrow(response, String.class);
        }
    }
}
