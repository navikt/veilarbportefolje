package no.nav.pto.veilarbportefolje.config;

import no.nav.pto.veilarbportefolje.aap.controller.AapClient;
import no.nav.common.auth.context.AuthContextHolder;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.client.aktoroppslag.CachedAktorOppslagClient;
import no.nav.common.client.aktoroppslag.PdlAktorOppslagClient;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.rest.client.RestClient;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.OppslagArbeidssoekerregisteretClient;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.VeilarbarenaClient;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtaksstotteClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.util.function.Supplier;

@Configuration
public class ClientConfig {

    static final String APPLICATION_NAME = "veilarbportefolje";

    @Bean
    public PoaoTilgangWrapper poaoTilgangWrapper(AuthContextHolder authContextHolder, AzureAdMachineToMachineTokenClient tokenClient, EnvironmentProperties environmentProperties) {
        return new PoaoTilgangWrapper(authContextHolder, tokenClient, environmentProperties);
    }

    @Bean
    public VeilarbVeilederClient veilarbVeilederClient(AuthService authService, EnvironmentProperties environmentProperties) {
        return new VeilarbVeilederClient(authService, environmentProperties);
    }

    @Bean
    public VedtaksstotteClient vedtaksstotteClient(
            AzureAdMachineToMachineTokenClient tokenClient,
            EnvironmentProperties environmentProperties
    ) {

        return new VedtaksstotteClient(
                environmentProperties.getVeilarbvedtaksstotteUrl(),
                () -> tokenClient.createMachineToMachineToken(environmentProperties.getVeilarbvedtaksstotteScope())
        );
    }

    @Bean
    public AapClient aapClient(
            AzureAdMachineToMachineTokenClient tokenClient,
            EnvironmentProperties environmentProperties
    ) {
        return new AapClient(
                environmentProperties.getAapUrl(),
                () -> tokenClient.createMachineToMachineToken(environmentProperties.getAapScope())
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
    public PdlClient pdlClient(AzureAdMachineToMachineTokenClient tokenClient, EnvironmentProperties environmentProperties) {
        String behandlingsnummer_oversikten = "B555";
        return new PdlClientImpl(
                environmentProperties.getPdlUrl(),
                () -> tokenClient.createMachineToMachineToken(environmentProperties.getPdlScope()), behandlingsnummer_oversikten
        );
    }

    @Bean
    public VeilarbarenaClient veilarbarenaMachineToMachineClient(
            AuthService authService,
            EnvironmentProperties environmentProperties
    ) {
        Supplier<String> tokenSupplier = () -> authService.getM2MToken(environmentProperties.getVeilarbarenaScope());

        return new VeilarbarenaClient(
                environmentProperties.getVeilarbarenaUrl(),
                tokenSupplier,
                RestClient.baseClient(),
                APPLICATION_NAME
        );
    }

    @Bean
    public OppslagArbeidssoekerregisteretClient oppslagArbeidssoekerregisteretClient(
            AuthService authService,
            EnvironmentProperties environmentProperties
    ) {
        Supplier<String> tokenSupplier = () -> authService.getM2MToken(environmentProperties.getOppslagArbeidssoekerregisteretScope());

        return new OppslagArbeidssoekerregisteretClient(
                environmentProperties.getOppslagArbeidssoekerregisteretUrl(),
                tokenSupplier,
                RestClient.baseClient(),
                APPLICATION_NAME
        );
    }
}
