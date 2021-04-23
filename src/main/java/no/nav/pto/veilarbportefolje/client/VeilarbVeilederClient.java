package no.nav.pto.veilarbportefolje.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.EnhetId;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.pto.veilarbportefolje.client.RestClientUtils.authHeaderMedSystemBruker;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
public class VeilarbVeilederClient {

    private final String url;
    private final OkHttpClient client;
    private final Cache<EnhetId, List<String> > hentVeilederePaaEnhetCache;

    public VeilarbVeilederClient(EnvironmentProperties environmentProperties) {
        this.url = environmentProperties.getVeilarbVeilederUrl();
        this.client = RestClient.baseClient();
        hentVeilederePaaEnhetCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.HOURS)
                .maximumSize(600)
                .build();
    }

    public List<String> hentVeilederePaaEnhet(EnhetId enhet) {
        return tryCacheFirst(hentVeilederePaaEnhetCache, enhet,
                () -> hentVeilederePaaEnhetQuery(enhet));
    }

    @SneakyThrows
    private List<String> hentVeilederePaaEnhetQuery(EnhetId enhet) {
        String path = format("/enhet/%s/identer", enhet);

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
