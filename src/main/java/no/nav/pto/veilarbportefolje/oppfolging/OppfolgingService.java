package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import java.util.function.Supplier;
import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.utils.UrlUtils.joinPaths;

@Slf4j
@Service
public class OppfolgingService {
    private final String veilarboppfolgingUrl;
    private final OkHttpClient client;
    private final AktorClient aktorClient;
    private final Supplier<String> systemUserTokenProvider;


    @Autowired
    public OppfolgingService(
            AktorClient aktorClient,
            AzureAdMachineToMachineTokenClient tokenClient,
            EnvironmentProperties environmentProperties
    ) {
        this.aktorClient = aktorClient;
        this.client = RestClient.baseClient();
        this.veilarboppfolgingUrl = environmentProperties.getVeilarboppfolgingUrl();
        systemUserTokenProvider = () -> tokenClient.createMachineToMachineToken(environmentProperties.getVeilarboppfolgingScope());
    }

    @SneakyThrows
    public boolean hentUnderOppfolging(AktorId aktorId) {
        Fnr fnr = aktorClient.hentFnr(aktorId);
        Request request = new Request.Builder()
                .url(joinPaths(veilarboppfolgingUrl, "/api/v3/hent-oppfolging"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + systemUserTokenProvider.get())
                .post(RequestBody.create(JsonUtils.toJson(new UnderOppfolgingRequest(fnr)), MEDIA_TYPE_JSON))
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.getBodyStr(response)
                    .map((bodyStr) -> JsonUtils.fromJson(bodyStr, UnderOppfolgingV2Response.class))
                    .map(r -> r.erUnderOppfolging)
                    .orElseThrow(() -> new IllegalStateException("Unable to parse json"));
        }
    }

}
