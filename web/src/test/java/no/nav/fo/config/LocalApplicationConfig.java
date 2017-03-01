package no.nav.fo.config;

import no.nav.fo.consumer.OppdaterBrukerdataListener;
import no.nav.fo.internal.IsAliveServlet;
import no.nav.fo.service.ServiceConfig;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration
@Import({
        OppdaterBrukerdataListener.class,
        DatabaseConfig.class,
        VirksomhetEnhetEndpointConfig.class,
        ServiceConfig.class,
        SolrConfig.class,
        MQMockConfig.class,
        AktoerEndpointConfig.class
})
public class LocalApplicationConfig {
    @Bean
    public IsAliveServlet isAliveServlet() {
        return new IsAliveServlet();
    }

    @Bean
    public OppdaterBrukerdataFletter tilordneVeilederFletter() { return new OppdaterBrukerdataFletter(); }
}
