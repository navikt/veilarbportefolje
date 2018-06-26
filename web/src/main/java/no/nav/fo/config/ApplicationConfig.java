package no.nav.fo.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.ApiApplication;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fo.filmottak.FilmottakConfig;
import no.nav.fo.internal.PingConfig;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.PepClientImpl;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import static no.nav.apiapp.ApiApplication.Sone.FSS;

@Slf4j
@EnableScheduling
@EnableAspectJAutoProxy
@Configuration
@Import({
        AbacContext.class,
        DatabaseConfig.class,
        VirksomhetEnhetEndpointConfig.class,
        ServiceConfig.class,
        ExternalServiceConfig.class,
        SolrConfig.class,
        FilmottakConfig.class,
        MetricsConfig.class,
        CacheConfig.class,
        PingConfig.class,
        FeedConfig.class,
        RestConfig.class,
        AktorConfig.class,
        VeilederServiceConfig.class,
        ClientConfig.class,
        RemoteFeatureConfig.class,
        DigitalKontaktinformasjonConfig.class
})
public class ApplicationConfig implements ApiApplication {
    public static final String APPLICATION_NAME = "veilarbportefolje";

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager() {
        return new JtaTransactionManager();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public PepClient pepClient(Pep pep) {
        return new PepClientImpl(pep);
    }

    @Bean
    public HovedindekseringScheduler hovedindekseringScheduler() {
        return new HovedindekseringScheduler();
    }
}
