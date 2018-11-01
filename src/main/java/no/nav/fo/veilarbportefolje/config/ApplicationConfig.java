package no.nav.fo.veilarbportefolje.config;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig;
import no.nav.fo.veilarbportefolje.filmottak.tiltak.TiltakHandler;
import no.nav.fo.veilarbportefolje.filmottak.tiltak.TiltakServlet;
import no.nav.fo.veilarbportefolje.filmottak.ytelser.KopierGR199FraArena;
import no.nav.fo.veilarbportefolje.filmottak.ytelser.YtelserServlet;
import no.nav.fo.veilarbportefolje.internal.PingConfig;
import no.nav.fo.veilarbportefolje.internal.PopulerIndekseringServlet;
import no.nav.fo.veilarbportefolje.internal.TotalHovedindekseringServlet;
import no.nav.fo.veilarbportefolje.service.PepClient;
import no.nav.fo.veilarbportefolje.service.PepClientImpl;
import no.nav.fo.veilarbportefolje.service.SolrService;
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

import static no.nav.apiapp.ServletUtil.leggTilServlet;
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
    public static final String AKTOER_V2_URL_PROPERTY = "AKTOER_V2_ENDPOINTURL";
    public static final String DIGITAL_KONTAKINFORMASJON_V1_URL_PROPERTY = "VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_ENDPOINTURL";
    public static final String VIRKSOMHET_ENHET_V1_URL_PROPERTY = "VIRKSOMHET_ENHET_V1_ENDPOINTURL";
    public static final String VEILARBPORTEFOLJE_SOLR_BRUKERCORE_URL_PROPERTY = "VEILARBPORTEFOLJE_SOLR_BRUKERCORE_URL";
    public static final String VEILARBPORTEFOLJE_SOLR_MASTERNODE_PROPERTY = "VEILARBPORTEFOLJE_SOLR_MASTERNODE";
    public static final String VEILARBOPPFOLGING_URL_PROPERTY = "VEILARBOPPFOLGINGAPI_URL";
    public static final String VEILARBAKTIVITET_URL_PROPERTY = "VEILARBAKTIVITETAPI_URL";
    public static final String VEILARBDIALOG_URL_PROPERTY = "VEILARBDIALOGAPI_URL";
    public static final String VEILARBVEILEDER_URL_PROPERTY = "VEILARBVEILEDERAPI_URL";
    public static final String VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY = "VEILARBLOGIN_REDIRECT_URL_URL";
    public static final String VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME_PROPERTY = "VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_USERNAME";
    public static final String VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD_PROPERTY = "VEILARBPORTEFOLJE_FILMOTTAK_SFTP_LOGIN_PASSWORD";
    public static final String ARENA_AKTIVITET_DATOFILTER_PROPERTY = "ARENA_AKTIVITET_DATOFILTER";

    @Inject
    private DataSource dataSource;

    @Inject
    private IndekseringScheduler hovedindekseringScheduler;

    @Inject
    private SolrService solrService;

    @Inject
    private TiltakHandler tiltakHandler;

    @Inject
    private KopierGR199FraArena kopierGR199FraArena;

    @Override
    public void startup(ServletContext servletContext) {
        setProperty("oppfolging.feed.brukertilgang", "srvveilarboppfolging", PUBLIC);
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();

        leggTilServlet(servletContext, new TotalHovedindekseringServlet(hovedindekseringScheduler), "/internal/totalhovedindeksering");
        leggTilServlet(servletContext, new PopulerIndekseringServlet(solrService), "/internal/populerindeks");
        leggTilServlet(servletContext, new TiltakServlet(tiltakHandler), "/internal/oppdatertiltak");
        leggTilServlet(servletContext, new YtelserServlet(kopierGR199FraArena), "/internal/oppdatertiltak");
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator
                .sts()
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
