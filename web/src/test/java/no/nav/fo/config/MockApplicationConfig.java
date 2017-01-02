package no.nav.fo.config;

import no.nav.fo.config.endpoint.norg.VirksomhetEnhetEndpointConfig;
import no.nav.fo.internal.HealthCheckService;
import no.nav.fo.internal.IsAliveServlet;
import no.nav.fo.service.ServiceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        Pingables.class,
        MockDatabaseConfig.class,
        VirksomhetEnhetEndpointConfig.class,
        ServiceConfig.class
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
