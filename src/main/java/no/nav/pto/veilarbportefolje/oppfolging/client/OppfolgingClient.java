package no.nav.pto.veilarbportefolje.oppfolging.client;

import lombok.SneakyThrows;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;

import java.util.function.Supplier;

import static no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON;
import static no.nav.common.utils.UrlUtils.joinPaths;

public class OppfolgingClient {
    private final AktorClient aktorClient;
    private final String baseUrl;
    private final OkHttpClient client;
    private final Supplier<String> machineToMachineTokenSupplier;

    public OppfolgingClient(
            AktorClient aktorClient,
            String baseUrl,
            Supplier<String> machineToMachineTokenSupplier
    ) {
        this.aktorClient = aktorClient;
        this.baseUrl = baseUrl;
        this.machineToMachineTokenSupplier = machineToMachineTokenSupplier;
        this.client = RestClient.baseClient();
    }

    @SneakyThrows
    public boolean hentUnderOppfolging(AktorId aktorId) {
        Fnr fnr = aktorClient.hentFnr(aktorId);
        Request request = new Request.Builder()
                .url(joinPaths(baseUrl, "/api/v3/hent-oppfolging"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + machineToMachineTokenSupplier.get())
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
