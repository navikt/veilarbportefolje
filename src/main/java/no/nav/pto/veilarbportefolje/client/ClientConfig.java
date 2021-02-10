package no.nav.pto.veilarbportefolje.client;

import no.nav.common.abac.*;
import no.nav.common.abac.audit.*;
import no.nav.common.client.pdl.*;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.common.utils.UrlUtils.createServiceUrl;


@Configuration
public class ClientConfig {

    @Bean
    public AktorOppslagClient aktorOppslagClient(SystemUserTokenProvider systemUserTokenProvider) {
        AktorOppslagClient aktorOppslagClient =  new PdlAktorOppslagClient(
                createServiceUrl("pdl-api", "default", false),
                AuthUtils::getInnloggetBrukerToken,
                systemUserTokenProvider::getSystemUserToken
        );
        return new CachedAktorOppslagClient(aktorOppslagClient);
    }

    @Bean
    public PdlClient pdlClient(SystemUserTokenProvider systemUserTokenProvider){
        return new PdlClientImpl(
                createServiceUrl("pdl-api", "default", false),
                systemUserTokenProvider::getSystemUserToken,
                systemUserTokenProvider::getSystemUserToken
                );
    }

    @Bean
    public MetricsClient metricsClient() {
        return new InfluxClient();
    }

    @Bean
    public VeilarbVeilederClient veilarbVeilederClient() {
        return new VeilarbVeilederClient();
    }

    @Bean
    public Pep veilarbPep(EnvironmentProperties properties) {
        Credentials serviceUserCredentials = getCredentials("service_user");
        return VeilarbPepFactory.get(
                properties.getAbacUrl(), serviceUserCredentials.username,
                serviceUserCredentials.password, new SpringAuditRequestInfoSupplier()
        );
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().build();
    }

}
