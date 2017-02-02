package no.nav.fo.config;

import no.nav.fo.consumer.EndringAvVeilederListener;
import no.nav.fo.internal.IsAliveServlet;
import no.nav.fo.service.ServiceConfig;
import org.springframework.context.annotation.*;
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
        MQMockConfig.class
})
public class LocalApplicationConfig {
    @Bean
    public IsAliveServlet isAliveServlet() {
        return new IsAliveServlet();
    }

}
