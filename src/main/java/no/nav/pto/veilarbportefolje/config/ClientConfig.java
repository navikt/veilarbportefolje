package no.nav.pto.veilarbportefolje.config;

import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPepFactory;
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.common.utils.UrlUtils;
import no.nav.poao_tilgang.client.TilgangClient;
import no.nav.poao_tilgang.client.TilgangHttpClient;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.DownstreamApi;
import no.nav.pto.veilarbportefolje.client.CachedTilgangClient;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.common.utils.UrlUtils.createDevInternalIngressUrl;
import static no.nav.common.utils.UrlUtils.createProdInternalIngressUrl;
import static no.nav.common.utils.UrlUtils.createServiceUrl;


@Configuration
public class ClientConfig {

    @Bean
    public TilgangClient tilgangClient(AzureAdMachineToMachineTokenClient tokenClient) {
        String url = isProduction() ?
                createProdInternalIngressUrl("poao-tilgang") :
                createDevInternalIngressUrl("poao-tilgang");

        String tokenScope = String.format("api://%s-gcp.poao.poao-tilgang/.default", isProduction() ? "prod" : "dev");

        TilgangHttpClient tilgangClient = new TilgangHttpClient(
                url,
                () -> tokenClient.createMachineToMachineToken(tokenScope),
                RestClient.baseClient()
        );

        return new CachedTilgangClient(tilgangClient);
    }

    @Bean
    public MetricsClient metricsClient() {
        return new InfluxClient();
    }

    @Bean
    public VeilarbVeilederClient veilarbVeilederClient(AuthService authService) {
        return new VeilarbVeilederClient(authService);
    }

    @Bean
    public VedtaksstotteClient vedtaksstotteClient(
            AuthService authService,
            AzureAdMachineToMachineTokenClient tokenClient
    ) {

        String tokenScope = String.format(
                "api://%s-fss.pto.veilarbvedtaksstotte/.default",
                isProduction() ? "prod" : "dev"
        );

        return new VedtaksstotteClient(
                createServiceUrl("veilarbvedtaksstotte", "pto", true),
                authService,
                () -> tokenClient.createMachineToMachineToken(tokenScope),
                new DownstreamApi(EnvironmentUtils.requireClusterName(), "pto", "veilarbvedtaksstotte"));
    }

    @Bean
    public Pep veilarbPep(EnvironmentProperties properties) {
        Credentials serviceUserCredentials = getCredentials("service_user");
        return VeilarbPepFactory.get(
                properties.getAbacVeilarbUrl(), serviceUserCredentials.username,
                serviceUserCredentials.password, new SpringAuditRequestInfoSupplier()
        );
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().build();
    }

    @Bean
    public AktorClient aktorClient(PdlClient pdlClient) {
        AktorOppslagClient aktorOppslagClient = new PdlAktorOppslagClient(pdlClient);

        return new AktorClient(new CachedAktorOppslagClient(aktorOppslagClient));
    }

    @Bean
    public PdlClient pdlClient(AzureAdMachineToMachineTokenClient tokenClient) {
        String tokenScop = String.format("api://%s-fss.pdl.pdl-api/.default",
                isProduction() ? "prod" : "dev"
        );
        return new PdlClientImpl(
                createServiceUrl("pdl-api", "pdl", false),
                () -> tokenClient.createMachineToMachineToken(tokenScop)
        );
    }

    private static boolean isProduction() {
        return EnvironmentUtils.isProduction().orElseThrow();
    }

    private String internalDevOrProdPdlIngress() {
        return isProduction()
                ? createProdInternalIngressUrl("pdl-api")
                : createDevInternalIngressUrl("pdl-api-q1");
    }
}
