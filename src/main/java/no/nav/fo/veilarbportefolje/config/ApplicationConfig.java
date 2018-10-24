package no.nav.fo.veilarbportefolje.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig;
import no.nav.fo.veilarbportefolje.internal.PingConfig;
import no.nav.fo.veilarbportefolje.service.PepClient;
import no.nav.fo.veilarbportefolje.service.PepClientImpl;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.*;

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
        DigitalKontaktinformasjonConfig.class
})
public class ApplicationConfig implements ApiApplication.NaisApiApplication {

    public static final String APPLICATION_NAME = "veilarbportefolje";

    @Inject
    private DataSource dataSource;

    @Override
    public void startup(ServletContext servletContext) {
        setProperty("oppfolging.feed.brukertilgang", "srvveilarboppfolging", PUBLIC);
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator
                .sts()
//                .azureADB2CLogin()
                .issoLogin()
        ;
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(DataSource ds) {
        return new DataSourceTransactionManager(ds);
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
    public IndekseringScheduler hovedindekseringScheduler() {
        return new IndekseringScheduler();
    }

    @Bean
    public UnleashService unleashService() {
        return new UnleashService(UnleashServiceConfig.builder()
                .applicationName(requireApplicationName())
                .unleashApiUrl(getRequiredProperty(UNLEASH_API_URL_PROPERTY_NAME))
                .build());
    }
}
