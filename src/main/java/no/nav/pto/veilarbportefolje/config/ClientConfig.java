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
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.token_client.client.AzureAdMachineToMachineTokenClient;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.util.VedtakstottePilotRequest;
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
    public AktorClient aktorClient(SystemUserTokenProvider systemUserTokenProvider) {
        AktorOppslagClient aktorOppslagClient = new PdlAktorOppslagClient(
                internalDevOrProdPdlIngress(),
                systemUserTokenProvider::getSystemUserToken,
                systemUserTokenProvider::getSystemUserToken
        );

        return new AktorClient(new CachedAktorOppslagClient(aktorOppslagClient));
    }

    @Bean
    public MetricsClient metricsClient() {
        return new InfluxClient();
    }

    @Bean
    public VeilarbVeilederClient veilarbVeilederClient(EnvironmentProperties environmentProperties) {
        return new VeilarbVeilederClient(environmentProperties);
    }

    @Bean
    public VedtakstottePilotRequest vedtakstottePilotRequest() {
        return new VedtakstottePilotRequest();
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
    public PdlClient pdlClient(AzureAdMachineToMachineTokenClient tokenClient) {
        String tokenScop = String.format("api://%s-fss.pdl.%s/.default",
                isProduction() ? "prod" : "dev",
                internalDevOrProdPdlIngress()
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
