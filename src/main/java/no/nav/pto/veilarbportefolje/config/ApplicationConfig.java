package no.nav.pto.veilarbportefolje.config;

import no.nav.common.abac.Pep;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.abac.audit.SpringAuditRequestInfoSupplier;
import no.nav.common.auth.oidc.filter.OidcAuthenticatorConfig;
import no.nav.common.auth.subject.IdentType;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.aktorregister.AktorregisterHttpClient;
import no.nav.common.client.aktorregister.CachedAktorregisterClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.featuretoggle.UnleashServiceConfig;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.aktviteter.KafkaAktivitetService;
import no.nav.pto.veilarbportefolje.arenafiler.FilmottakConfig;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.YtelserServlet;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakServlet;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.elastic.ElasticConfig;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.IndekseringScheduler;
import no.nav.pto.veilarbportefolje.elastic.MetricsReporter;
import no.nav.pto.veilarbportefolje.feedconsumer.FeedConfig;
import no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.internal.*;
import no.nav.pto.veilarbportefolje.kafka.KafkaConfig;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerRunnable;
import no.nav.pto.veilarbportefolje.krr.DigitalKontaktinformasjonConfig;
import no.nav.pto.veilarbportefolje.krr.KrrService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService;
import no.nav.pto.veilarbportefolje.service.VeilederService;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import static no.nav.common.utils.EnvironmentUtils.requireNamespace;
import static no.nav.common.utils.NaisUtils.getCredentials;


@EnableScheduling
@EnableAspectJAutoProxy
@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
public class ApplicationConfig {

    public static final String APPLICATION_NAME = "veilarbportefolje";
    public static final String AKTOER_V2_URL_PROPERTY = "AKTOER_V2_ENDPOINTURL";
    public static final String DIGITAL_KONTAKINFORMASJON_V1_URL_PROPERTY = "VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1_ENDPOINTURL";
    public static final String VIRKSOMHET_ENHET_V1_URL_PROPERTY = "VIRKSOMHET_ENHET_V1_ENDPOINTURL";
    public static final String VEILARBOPPFOLGING_URL_PROPERTY = "VEILARBOPPFOLGINGAPI_URL";
    public static final String VEILARBAKTIVITET_URL_PROPERTY = "VEILARBAKTIVITETAPI_URL";
    public static final String ARENA_AKTIVITET_DATOFILTER_PROPERTY = "ARENA_AKTIVITET_DATOFILTER";
    public static final String SKIP_DB_MIGRATION_PROPERTY = "SKIP_DB_MIGRATION";
    public static final String ELASTICSEARCH_USERNAME_PROPERTY = "VEILARBELASTIC_USERNAME";
    public static final String ELASTICSEARCH_PASSWORD_PROPERTY = "VEILARBELASTIC_PASSWORD";
    public static final String SECURITYTOKENSERVICE_URL_PROPERTY_NAME = "SECURITYTOKENSERVICE_URL";

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

    @Inject
    private KafkaAktivitetService kafkaAktivitetService;

    @Inject
    private CvService cvService;

    public void startup(ServletContext servletContext) {
        setProperty("oppfolging.feed.brukertilgang", "srvveilarboppfolging");

        new KafkaConsumerRunnable<>(
                kafkaAktivitetService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                KafkaConfig.Topic.KAFKA_AKTIVITER_CONSUMER_TOPIC,
                "portefolje.kafka.aktiviteter"
        );

        new KafkaConsumerRunnable<>(
                vedtakService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                KafkaConfig.Topic.VEDTAK_STATUS_ENDRING_TOPIC,
                "veilarbportfolje-hent-data-fra-vedtakstotte"
        );

        new KafkaConsumerRunnable<>(
                oppfolgingService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                KafkaConfig.Topic.OPPFOLGING_CONSUMER_TOPIC,
                KafkaConfig.KAFKA_OPPFOLGING_TOGGLE
        );

        new KafkaConsumerRunnable<>(
                dialogService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                KafkaConfig.Topic.DIALOG_CONSUMER_TOPIC,
               "veilarbdialog.kafka"
        );

        new KafkaConsumerRunnable<>(
                cvService,
                unleashService,
                KafkaProperties.kafkaMedAvroProperties(),
                KafkaConfig.Topic.CV_ENDRET_TOPIC,
                "veilarbportefolje.kafka.cv.killswitch"
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

    public void configure(ApiAppConfigurator apiAppConfigurator) {
        apiAppConfigurator
                .selfTests(KafkaConfig.getHelseSjekker())
                .addOidcAuthenticator(createOpenAmAuthenticatorConfig())
                .addOidcAuthenticator(createSystemUserAuthenticatorConfig())
                .sts();
    }

    private OidcAuthenticatorConfig createOpenAmAuthenticatorConfig() {
        String discoveryUrl = getRequiredProperty("OPENAM_DISCOVERY_URL");
        String clientId = getRequiredProperty("VEILARBLOGIN_OPENAM_CLIENT_ID");
        String refreshUrl = getRequiredProperty("VEILARBLOGIN_OPENAM_REFRESH_URL");

        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(discoveryUrl)
                .withClientId(clientId)
                .withRefreshUrl(refreshUrl)
                .withRefreshTokenCookieName(REFRESH_TOKEN_COOKIE_NAME)
                .withIdTokenCookieName(OPEN_AM_ID_TOKEN_COOKIE_NAME)
                .withIdentType(IdentType.InternBruker);
    }

    private OidcAuthenticatorConfig createSystemUserAuthenticatorConfig() {
        String discoveryUrl = getRequiredProperty("SECURITY_TOKEN_SERVICE_DISCOVERY_URL");
        String clientId = getRequiredProperty("SECURITY_TOKEN_SERVICE_CLIENT_ID");

        return new OidcAuthenticatorConfig()
                .withDiscoveryUrl(discoveryUrl)
                .withClientId(clientId)
                .withIdentType(IdentType.Systemressurs);
    }

    @Bean
    public MetricsReporter elasticMetricsReporter(ElasticIndexer elasticIndexer) {
        return new MetricsReporter(elasticIndexer);
    }

    @Bean
    public VeilederService veilederservice() {
        return new VeilederService(requireNamespace());
    }


    @Bean
    public UnleashService unleashService() {
        return new UnleashService(UnleashServiceConfig.resolveFromEnvironment());
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

    @Bean
    public Pep veilarbPep(EnvironmentProperties properties) {
        Credentials serviceUserCredentials = getCredentials("service_user");
        return new VeilarbPep(
                properties.getAbacUrl(), serviceUserCredentials.username,
                serviceUserCredentials.password, new SpringAuditRequestInfoSupplier()
        );
    }

    @Bean
    public AktorregisterClient aktorregisterClient(EnvironmentProperties properties, SystemUserTokenProvider tokenProvider) {
        AktorregisterClient aktorregisterClient = new AktorregisterHttpClient(
                properties.getAktorregisterUrl(), APPLICATION_NAME, tokenProvider::getSystemUserToken
        );
        return new CachedAktorregisterClient(aktorregisterClient);
    }
}
