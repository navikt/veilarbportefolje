package no.nav.pto.veilarbportefolje.client;

import no.nav.common.abac.*;
import no.nav.common.abac.audit.*;
import no.nav.common.client.pdl.AktorOppslagClient;
import no.nav.common.client.pdl.CachedAktorOppslagClient;
import no.nav.common.client.pdl.PdlAktorOppslagClient;
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


@Configuration
public class ClientConfig {

    @Bean
    public AktorOppslagClient aktorOppslagClient(EnvironmentProperties properties, SystemUserTokenProvider systemUserTokenProvider) {
        AktorOppslagClient aktorOppslagClient =  new PdlAktorOppslagClient(
                properties.getPdlUrl(),
                AuthUtils::getInnloggetBrukerToken,
                systemUserTokenProvider::getSystemUserToken
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
                serviceUserCredentials.username,
                new AbacCachedClient(new AbacHttpClient(properties.getAbacUrl(), serviceUserCredentials.username, serviceUserCredentials.password)),
                new NimbusSubjectProvider(),
                new AuditConfig(new AuditLogger(), new SpringAuditRequestInfoSupplier(),null)
        );
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder().build();
    }

}
