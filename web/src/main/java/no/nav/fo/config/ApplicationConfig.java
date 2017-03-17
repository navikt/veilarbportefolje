package no.nav.fo.config;

import no.nav.fo.consumer.OppdaterBrukerdataListener;
import no.nav.fo.internal.IsAliveServlet;
import no.nav.fo.service.ServiceConfig;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration
@Import({
        AbacContext.class,
        OppdaterBrukerdataListener.class,
        DatabaseConfig.class,
        VirksomhetEnhetEndpointConfig.class,
        ServiceConfig.class,
        SolrConfig.class,
        MessageQueueConfig.class,
        AktoerEndpointConfig.class
})
public class ApplicationConfig {

    @Bean
    public IsAliveServlet isAliveServlet() {
        return new IsAliveServlet();
    }

    @Bean
    public OppdaterBrukerdataFletter tilordneVeilederFletter() { return new OppdaterBrukerdataFletter(); }
}
