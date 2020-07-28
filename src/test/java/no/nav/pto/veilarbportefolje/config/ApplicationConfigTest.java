package no.nav.pto.veilarbportefolje.config;

import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.krr.DigitalKontaktinformasjonConfig;
import no.nav.pto.veilarbportefolje.mock.AktorregisterClientMock;
import no.nav.pto.veilarbportefolje.mock.LeaderElectionClientMock;
import no.nav.pto.veilarbportefolje.mock.MetricsClientMock;
import no.nav.pto.veilarbportefolje.mock.UnleashServiceMock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static no.nav.common.utils.NaisUtils.getCredentials;
import static org.mockito.Mockito.mock;

@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
@Import({
        DatabaseConfigTest.class,
        FilmottakConfig.class,
        ServiceConfigTest.class
})
public class ApplicationConfigTest {

    @Bean
    public Credentials serviceUserCredentials() {
        Credentials serviceUserCredentials = new Credentials("username", "password");
        return serviceUserCredentials;
    }

    @Bean
    public ElasticIndexer elasticIndexer() {
        return mock(ElasticIndexer.class);
    }

    @Bean
    public AktorregisterClient aktorregisterClient() {
        return new AktorregisterClientMock();
    }

    @Bean
    public LockingTaskExecutor lockingTaskExecutor() {
        return mock(LockingTaskExecutor.class);
    }

    @Bean
    public UnleashService unleashService() {
        return mock(UnleashService.class);
    }

    @Bean
    public MetricsClient metricsClient() { return new MetricsClientMock(); }

    @Bean
    public LeaderElectionClient leaderElectionClient() {
        return new LeaderElectionClientMock();
    }

}
