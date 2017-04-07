package no.nav.fo.config;

import no.nav.fo.consumer.OppdaterBrukerdataListener;
import no.nav.fo.internal.IsAliveServlet;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
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
        ArenafilConfig.class,
        MetricsConfig.class,
        AktoerEndpointConfig.class,
        AbacContext.class,
        CacheConfig.class
})
public class LocalApplicationConfig {
    @Bean
    public IsAliveServlet isAliveServlet() {
        return new IsAliveServlet();
    }

    @Bean
    public OppdaterBrukerdataFletter tilordneVeilederFletter() {
        return new OppdaterBrukerdataFletter();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
