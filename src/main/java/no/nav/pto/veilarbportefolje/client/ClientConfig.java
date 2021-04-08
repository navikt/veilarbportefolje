package no.nav.pto.veilarbportefolje.client;

import no.nav.common.abac.*;
import no.nav.common.abac.audit.*;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.aktorregister.AktorregisterHttpClient;
import no.nav.common.client.aktorregister.CachedAktorregisterClient;
import no.nav.common.client.pdl.AktorOppslagClient;
import no.nav.common.client.pdl.CachedAktorOppslagClient;
import no.nav.common.client.pdl.PdlAktorOppslagClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.InfluxClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.admin.ComparatorForAktorIdClients;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;

import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.common.utils.UrlUtils.createServiceUrl;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.APPLICATION_NAME;


@Configuration
public class ClientConfig {

    @Bean
    public AktorClient aktorClient(EnvironmentProperties properties, SystemUserTokenProvider systemUserTokenProvider, UnleashService unleashService) {
        AktorOppslagClient aktorOppslagClient = new PdlAktorOppslagClient(
                createServiceUrl("pdl-api", "default", false),
                AuthUtils::getInnloggetBrukerToken,
                systemUserTokenProvider::getSystemUserToken
        );
        AktorregisterClient aktorregisterClient = new AktorregisterHttpClient(
                properties.getAktorregisterUrl(), APPLICATION_NAME, systemUserTokenProvider::getSystemUserToken
        );

        return new AktorClient(new CachedAktorOppslagClient(aktorOppslagClient), new CachedAktorregisterClient(aktorregisterClient), unleashService);
    }

    @Bean
    public ComparatorForAktorIdClients comparatorForAktorIdClients(EnvironmentProperties properties, SystemUserTokenProvider systemUserTokenProvider, BrukerRepository brukerRepository){
        AktorOppslagClient aktorOppslagClient = new PdlAktorOppslagClient(
                createServiceUrl("pdl-api", "default", false),
                AuthUtils::getInnloggetBrukerToken,
                systemUserTokenProvider::getSystemUserToken
        );
        AktorregisterClient aktorregisterClient = new AktorregisterHttpClient(
                properties.getAktorregisterUrl(), APPLICATION_NAME, systemUserTokenProvider::getSystemUserToken
        );

        return new ComparatorForAktorIdClients(new CachedAktorOppslagClient(aktorOppslagClient), new CachedAktorregisterClient(aktorregisterClient), brukerRepository);
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
