package no.nav.fo.config;

import no.nav.apiapp.ApiApplication;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fo.config.unleash.UnleashSpringConfig;
import no.nav.fo.filmottak.FilmottakConfig;
import no.nav.fo.service.PepClient;
import no.nav.fo.service.PepClientImpl;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static no.nav.apiapp.ApiApplication.Sone.FSS;
import static no.nav.fo.StartJettyVeilArbPortefolje.APPLICATION_NAME;

@EnableAspectJAutoProxy
@EnableScheduling
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
        RestConfig.class,
        AktorConfig.class,
        FeedConfig.class,
        VeilederServiceConfig.class,
        ClientConfig.class,
        DigitalKontaktinformasjonConfig.class,
        UnleashSpringConfig.class
})
public class LocalApplicationConfig implements ApiApplication {

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public PepClient pepClient(Pep pep) {
        return new PepClientImpl(pep);
    }

    @Override
    public String getApplicationName() {
        return APPLICATION_NAME;
    }

    @Override
    public Sone getSone() {
        return FSS;
    }

    @Bean
    public HovedindekseringScheduler hovedindekseringScheduler() {
        return new HovedindekseringScheduler();
    }
}
