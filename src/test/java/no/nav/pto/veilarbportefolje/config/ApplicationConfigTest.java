package no.nav.pto.veilarbportefolje.config;

import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.mock.AktorregisterClientMock;
import no.nav.pto.veilarbportefolje.mock.MetricsClientMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

@Configuration
@Import({
        DatabaseConfigTest.class
})
public class ApplicationConfigTest {

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

}
