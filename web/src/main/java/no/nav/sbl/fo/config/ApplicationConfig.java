package no.nav.sbl.fo.config;

import no.nav.sbl.fo.config.endpoint.norg.VirksomhetEnhetEndpointConfig;
import no.nav.sbl.fo.internal.HealthCheckService;
import no.nav.sbl.fo.internal.IsAliveServlet;
import no.nav.sbl.fo.service.ServiceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        Pingables.class,
        DatabaseConfig.class,
        VirksomhetEnhetEndpointConfig.class,
        ServiceConfig.class
})
public class ApplicationConfig {

    @Bean
    public IsAliveServlet isAliveServlet() {
        return new IsAliveServlet();
    }

    @Bean
    public HealthCheckService healthCheckService() {
        return new HealthCheckService();
    }
}
