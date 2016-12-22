package no.nav.sbl.fo.config;

import no.nav.sbl.fo.config.endpoint.norg.VirksomhetEnhetEndpointConfig;
import no.nav.sbl.fo.internal.HealthCheckService;
import no.nav.sbl.fo.internal.IsAliveServlet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        Pingables.class,
        MockDatabaseConfig.class,
        VirksomhetEnhetEndpointConfig.class
})
public class MockApplicationConfig {
    @Bean
    public IsAliveServlet isAliveServlet() {
        return new IsAliveServlet();
    }

    @Bean
    public HealthCheckService healthCheckService() {
        return new HealthCheckService();
    }

}
