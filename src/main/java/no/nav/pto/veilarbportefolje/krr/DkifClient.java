package no.nav.pto.veilarbportefolje.krr;

import lombok.SneakyThrows;
import no.nav.pto.veilarbportefolje.config.ApplicationConfig;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;

import static java.time.Duration.ofSeconds;
import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.common.utils.UrlUtils.joinPaths;
import static no.nav.pto.veilarbportefolje.client.RestClientUtils.authHeaderMedSystemBruker;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@Component
public class DkifClient {

    private final HttpClient httpClient;
    private final EnvironmentProperties env;

    public DkifClient(HttpClient httpClient, EnvironmentProperties environmentProperties) {
        this.httpClient = httpClient;
        this.env = environmentProperties;
    }

    @SneakyThrows
    public Optional<DkifKontaktinfoDTO> hentKontaktInfo(AktoerId aktoerid) {
        final String uri = joinPaths(env.getDkifUrl(), "/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=false");

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(ofSeconds(10))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header("Nav-Call-Id", UUID.randomUUID().toString())
                .header("Nav-Consumer-Id", ApplicationConfig.APPLICATION_NAME)
                .header("Nav-Personidenter", aktoerid.toString())
                .header("Authorization", authHeaderMedSystemBruker())
                .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        final DkifKontaktinfoDTO dto = fromJson(response.body(), DkifKontaktinfoDTO.class);
        return Optional.ofNullable(dto);
    }

}
