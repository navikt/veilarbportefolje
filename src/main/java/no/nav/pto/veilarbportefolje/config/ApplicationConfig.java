package no.nav.pto.veilarbportefolje.config;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.featuretoggle.UnleashServiceConfig;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.common.leaderelection.LeaderElectionHttpClient;
import no.nav.common.sts.NaisSystemUserTokenProvider;
import no.nav.common.sts.OpenAmSystemUserTokenProvider;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.arenafiler.gr199.ytelser.KopierGR199FraArena;
import no.nav.pto.veilarbportefolje.arenafiler.gr202.tiltak.TiltakHandler;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.IndekseringScheduler;
import no.nav.pto.veilarbportefolje.elastic.MetricsReporter;
import no.nav.pto.veilarbportefolje.krr.KrrService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import static no.nav.common.utils.NaisUtils.getCredentials;


@EnableScheduling
@Configuration
@EnableConfigurationProperties({EnvironmentProperties.class})
public class ApplicationConfig {

    public static final String APPLICATION_NAME = "veilarbportefolje";
    public static final String VEILARBOPPFOLGING_URL_PROPERTY = "VEILARBOPPFOLGINGAPI_URL";
    public static final String ARENA_AKTIVITET_DATOFILTER_PROPERTY = "ARENA_AKTIVITET_DATOFILTER";
    public static final String ELASTICSEARCH_USERNAME_PROPERTY = "VEILARBELASTIC_USERNAME";
    public static final String ELASTICSEARCH_PASSWORD_PROPERTY = "VEILARBELASTIC_PASSWORD";


    @Bean
    public MetricsReporter elasticMetricsReporter(ElasticIndexer elasticIndexer) {
        return new MetricsReporter(elasticIndexer);
    }

    @Bean
    public UnleashService unleashService() {
        return new UnleashService(UnleashServiceConfig.resolveFromEnvironment());
    }

    @Bean
    public IndekseringScheduler indekseringScheduler(ElasticIndexer elasticIndexer, TiltakHandler tiltakHandler, KopierGR199FraArena kopierGR199FraArena, KrrService krrService, LeaderElectionClient leaderElectionClient) {
        return new IndekseringScheduler(elasticIndexer, tiltakHandler, kopierGR199FraArena, krrService, leaderElectionClient);
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ConcurrentTaskScheduler scheduler = new ConcurrentTaskScheduler();
        scheduler.setErrorHandler(new ScheduledErrorHandler());
        return scheduler;
    }

    @Bean
    public Credentials serviceUserCredentials() {
        Credentials serviceUserCredentials = getCredentials("service_user");
        return serviceUserCredentials;
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider(EnvironmentProperties properties, Credentials serviceUserCredentials) {
        return new NaisSystemUserTokenProvider(properties.getStsDiscoveryUrl(), serviceUserCredentials.username, serviceUserCredentials.password);
    }

    @Bean
    public LeaderElectionClient leaderElectionClient() {
        return new LeaderElectionHttpClient();
    }
}
