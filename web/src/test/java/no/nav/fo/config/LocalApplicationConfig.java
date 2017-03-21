package no.nav.fo.config;

import no.nav.fo.consumer.IndekserYtelserHandler;
import no.nav.fo.consumer.KopierGR199FraArena;
import no.nav.fo.consumer.OppdaterBrukerdataListener;
import no.nav.fo.internal.IsAliveServlet;
import no.nav.fo.service.ServiceConfig;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAspectJAutoProxy
@EnableScheduling
@Configuration
@Import({
        OppdaterBrukerdataListener.class,
        DatabaseConfig.class,
        VirksomhetEnhetEndpointConfigMock.class,
        ServiceConfig.class,
        SolrConfig.class,
        MQMockConfig.class,
        AktoerEndpointConfig.class,
        MetricsConfig.class
})
public class LocalApplicationConfig {
    @Bean
    public IsAliveServlet isAliveServlet() {
        return new IsAliveServlet();
    }

    @Bean
    public OppdaterBrukerdataFletter tilordneVeilederFletter() { return new OppdaterBrukerdataFletter(); }

    @Bean
    public IndekserYtelserHandler indekserYtelserHandler() {
        return new IndekserYtelserHandler();
    }

    @Bean
    public KopierGR199FraArena kopierGR199FraArena(IndekserYtelserHandler indekserYtelserHandler) {
        return new KopierGR199FraArena(indekserYtelserHandler);
    }
}
