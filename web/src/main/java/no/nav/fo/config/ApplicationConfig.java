package no.nav.fo.config;

import no.nav.fo.consumer.EndringAvVeilederListener;
import no.nav.fo.internal.IsAliveServlet;
import no.nav.fo.service.ServiceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration
@Import({
        EndringAvVeilederListener.class,
        DatabaseConfig.class,
        VirksomhetEnhetEndpointConfig.class,
        ServiceConfig.class,
        SolrConfig.class,
        PortefoljeMock.class,
        MessageQueueConfig.class
})
public class ApplicationConfig {

    @Bean
    public IsAliveServlet isAliveServlet() {
        return new IsAliveServlet();
    }
}
