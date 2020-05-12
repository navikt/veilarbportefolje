package no.nav.pto.veilarbportefolje.config;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.config.ApiAppConfigurator;
import no.nav.brukerdialog.security.oidc.provider.SecurityTokenServiceOidcProvider;
import no.nav.brukerdialog.security.oidc.provider.SecurityTokenServiceOidcProviderConfig;
import no.nav.dialogarena.aktor.AktorConfig;
import no.nav.pto.veilarbportefolje.abac.PepClient;
import no.nav.pto.veilarbportefolje.abac.PepClientImpl;
import no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.YtelserServlet;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakServlet;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.elastic.ElasticConfig;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.IndekseringScheduler;
import no.nav.pto.veilarbportefolje.elastic.MetricsReporter;
import no.nav.pto.veilarbportefolje.feed.FeedConfig;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.internal.*;
import no.nav.pto.veilarbportefolje.kafka.KafkaConfig;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerRunnable;
import no.nav.pto.veilarbportefolje.krr.DigitalKontaktinformasjonConfig;
import no.nav.pto.veilarbportefolje.krr.KrrService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService;
import no.nav.pto.veilarbportefolje.service.VeilederService;
import no.nav.pto.veilarbportefolje.vedtakstotte.KafkaVedtakStatusEndring;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import no.nav.sbl.dialogarena.common.abac.pep.Pep;
import no.nav.sbl.dialogarena.common.abac.pep.context.AbacContext;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig;
import org.apache.yetus.audience.InterfaceStability;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.transaction.PlatformTransactionManager;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.client.Client;

import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import static no.nav.apiapp.ServletUtil.leggTilServlet;
import static no.nav.brukerdialog.security.oidc.provider.SecurityTokenServiceOidcProviderConfig.STS_OIDC_CONFIGURATION_URL_PROPERTY;
import static no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig.UNLEASH_API_URL_PROPERTY_NAME;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.*;

@EnableScheduling
@EnableAspectJAutoProxy
@Configuration
@Slf4j
@Import({
        AbacContext.class,
        DatabaseConfig.class,
        VirksomhetEnhetEndpointConfig.class,
        ServiceConfig.class,
        ExternalServiceConfig.class,
        FilmottakConfig.class,
        MetricsConfig.class,
        CacheConfig.class,
        FeedConfig.class,
        AktorConfig.class,
        ClientConfig.class,
        DigitalKontaktinformasjonConfig.class,
        ScheduledErrorHandler.class,
        ElasticConfig.class,
        ControllerConfig.class,
        KafkaConfig.class
})
public class ApplicationConfig implements ApiApplication {

    public static final String APPLICATION_NAME = "veilarbportefolje";
    public static final String AKTOER_V2_URL_PROPERTY = "AKTOER_V2_ENDPOINTURL";
    public static final String DIGITAL_KONTAKINFORMASJON_V1_URL_PROPERTY = "VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_ENDPOINTURL";
    public static final String VIRKSOMHET_ENHET_V1_URL_PROPERTY = "VIRKSOMHET_ENHET_V1_ENDPOINTURL";
    public static final String VEILARBOPPFOLGING_URL_PROPERTY = "VEILARBOPPFOLGINGAPI_URL";
    public static final String VEILARBAKTIVITET_URL_PROPERTY = "VEILARBAKTIVITETAPI_URL";
    public static final String VEILARBDIALOG_URL_PROPERTY = "VEILARBDIALOGAPI_URL";
    public static final String VEILARBVEILEDER_URL_PROPERTY = "VEILARBVEILEDERAPI_URL";
    public static final String VEILARBLOGIN_REDIRECT_URL_URL_PROPERTY = "VEILARBLOGIN_REDIRECT_URL_URL";
    public static final String ARENA_AKTIVITET_DATOFILTER_PROPERTY = "ARENA_AKTIVITET_DATOFILTER";
    public static final String SKIP_DB_MIGRATION_PROPERTY = "SKIP_DB_MIGRATION";
    public static final String ELASTICSEARCH_USERNAME_PROPERTY = "VEILARBELASTIC_USERNAME";
    public static final String ELASTICSEARCH_PASSWORD_PROPERTY = "VEILARBELASTIC_PASSWORD";

    @Inject
    private DataSource dataSource;

    @Inject
    private TiltakHandler tiltakHandler;

    @Inject
    private KopierGR199FraArena kopierGR199FraArena;

    @Inject
    private ElasticIndexer elasticIndexer;

    @Inject
    private KrrService krrService;

    @Inject
    private OppfolgingRepository oppfolgingRepository;

    @Inject
    private BrukerRepository brukerRepository;

    @Inject
    private AktivitetDAO aktivitetDAO;

    @Inject
    private OppfolgingService oppfolgingService;

    @Inject
    private UnleashService unleashService;

    @Inject
    private DialogService dialogService;

    @Inject
    private VedtakService vedtakService;

    @Override
    public void startup(ServletContext servletContext) {
        setProperty("oppfolging.feed.brukertilgang", "srvveilarboppfolging", PUBLIC);

        if (!skipDbMigration()) {
            Flyway flyway = new Flyway();
            flyway.setDataSource(dataSource);
            flyway.migrate();
        }

        new KafkaConsumerRunnable(
                vedtakService,
                unleashService,
                KafkaConfig.Topic.VEDTAK_STATUS_ENDRING_TOPIC,
                Optional.of("veilarbportfolje-hent-data-fra-vedtakstotte")
        );

        new KafkaConsumerRunnable(
                oppfolgingService,
                unleashService,
                KafkaConfig.Topic.OPPFOLGING_CONSUMER_TOPIC,
                Optional.of(KafkaConfig.KAFKA_OPPFOLGING_TOGGLE)
        );

        new KafkaConsumerRunnable(
                dialogService,
                unleashService,
                KafkaConfig.Topic.DIALOG_CONSUMER_TOPIC,
                Optional.of("veilarbdialog.kafka")
        );

        leggTilServlet(servletContext, new ArenaFilerIndekseringServlet(elasticIndexer, tiltakHandler, kopierGR199FraArena), "/internal/totalhovedindeksering");
        leggTilServlet(servletContext, new TiltakServlet(tiltakHandler), "/internal/oppdater_tiltak");
        leggTilServlet(servletContext, new YtelserServlet(kopierGR199FraArena), "/internal/oppdater_ytelser");
        leggTilServlet(servletContext, new PopulerElasticServlet(elasticIndexer), "/internal/populer_elastic");
        leggTilServlet(servletContext, new PopulerKrrServlet(krrService), "/internal/populer_krr");
        leggTilServlet(servletContext, new ResetOppfolgingFeedServlet(oppfolgingRepository), "/internal/reset_feed_oppfolging");
        leggTilServlet(servletContext, new ResetAktivitetFeedServlet(brukerRepository), "/internal/reset_feed_aktivitet");
        leggTilServlet(servletContext, new SlettAktivitetServlet(aktivitetDAO, elasticIndexer), "/internal/slett_aktivitet");
    }

    private Boolean skipDbMigration() {
        return getOptionalProperty(SKIP_DB_MIGRATION_PROPERTY)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    @Override
    public void configure(ApiAppConfigurator apiAppConfigurator) {

        SecurityTokenServiceOidcProvider securityTokenServiceOidcProvider = new SecurityTokenServiceOidcProvider(SecurityTokenServiceOidcProviderConfig.builder()
                .discoveryUrl(getRequiredProperty(STS_OIDC_CONFIGURATION_URL_PROPERTY))
                .build());

        apiAppConfigurator
                .sts()
                .selfTests(KafkaConfig.getHelseSjekker())
                .issoLogin()
                .oidcProvider(securityTokenServiceOidcProvider);
    }

    @Bean
    public MetricsReporter elasticMetricsReporter(ElasticIndexer elasticIndexer) {
        return new MetricsReporter(elasticIndexer);
    }

    @Bean
    public VeilederService veilederservice(Client restClient) {
        return new VeilederService(restClient);
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
    public UnleashService unleashService() {
        return new UnleashService(UnleashServiceConfig.builder()
                .applicationName(APPLICATION_NAME)
                .unleashApiUrl(getRequiredProperty(UNLEASH_API_URL_PROPERTY_NAME))
                .build());
    }


    @Bean
    public IndekseringScheduler indekseringScheduler(ElasticIndexer elasticIndexer, TiltakHandler tiltakHandler, KopierGR199FraArena kopierGR199FraArena, KrrService krrService) {
        return new IndekseringScheduler(elasticIndexer, tiltakHandler, kopierGR199FraArena, krrService);
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ConcurrentTaskScheduler scheduler = new ConcurrentTaskScheduler();
        scheduler.setErrorHandler(new ScheduledErrorHandler());
        return scheduler;
    }
}
