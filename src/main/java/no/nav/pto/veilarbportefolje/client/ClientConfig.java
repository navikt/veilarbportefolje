package no.nav.pto.veilarbportefolje.client;

import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier;
import no.nav.common.client.pdl.AktorOppslagClient;
import no.nav.common.client.pdl.CachedAktorOppslagClient;
import no.nav.common.client.pdl.PdlAktorOppslagClient;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.APPLICATION_NAME;


@Configuration
public class ClientConfig {

    @Bean
    public AktorOppslagClient aktorOppslagClient(EnvironmentProperties properties, SystemUserTokenProvider systemUserTokenProvider) {
        AktorOppslagClient aktorOppslagClient =  new PdlAktorOppslagClient(
                properties.getPdlUrl(),
                systemUserTokenProvider::getSystemUserToken,
                () -> APPLICATION_NAME
        );
        return new CachedAktorOppslagClient(aktorOppslagClient);
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
        return new VeilarbPep(
                properties.getAbacUrl(), serviceUserCredentials.username,
                serviceUserCredentials.password, new SpringAuditRequestInfoSupplier()
        );
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().build();
    }

}
