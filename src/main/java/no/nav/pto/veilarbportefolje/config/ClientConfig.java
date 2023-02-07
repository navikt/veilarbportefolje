package no.nav.pto.veilarbportefolje.config;

import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPepFactory;
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper;
import no.nav.pto.veilarbportefolje.client.ClientUtils;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

import static no.nav.common.utils.NaisUtils.getCredentials;


@Configuration
public class ClientConfig {

    @Bean
    public PoaoTilgangWrapper poaoTilgangWrapper(AuthContextHolder authContextHolder, AzureAdMachineToMachineTokenClient tokenClient) {
        return new PoaoTilgangWrapper(authContextHolder, tokenClient);
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

        return new VedtaksstotteClient(
                ClientUtils.getVeilarbvedtaksstotteServiceUrl(),
                authService,
                () -> tokenClient.createMachineToMachineToken(ClientUtils.getVeilarbvedtaksstotteTokenScope(EnvironmentUtils.isProduction().orElseThrow())),
                ClientUtils.getVeilarbvedtakstotteDownstreamApi(EnvironmentUtils.requireClusterName()));
    }

    @Bean
    public Pep veilarbPep(EnvironmentProperties properties) {
        Credentials serviceUserCredentials = getCredentials("service_user");
        return VeilarbPepFactory.get(
                properties.getAbacVeilarbUrl(),
                serviceUserCredentials.username,
                serviceUserCredentials.password,
                new SpringAuditRequestInfoSupplier()
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
        return new PdlClientImpl(
                ClientUtils.getPdlServiceUrl(),
                () -> tokenClient.createMachineToMachineToken(ClientUtils.getPdlM2MTokenScope(EnvironmentUtils.isProduction().orElseThrow()))
        );
    }

}
