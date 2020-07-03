package no.nav.pto.veilarbportefolje.elastic;
<<<<<<< HEAD
import no.nav.common.health.HealthCheckResult;
=======
import no.nav.pto.veilarbportefolje.config.DatabaseConfig;
import no.nav.common.featuretoggle.UnleashService;
>>>>>>> 8e6104206d2c3db1266862aa3a77000ff430366a
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.database.DatabaseConfig;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.auth.PepClient;
<<<<<<< HEAD
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
=======
import no.nav.pto.veilarbportefolje.service.VeilederService;
>>>>>>> 8e6104206d2c3db1266862aa3a77000ff430366a
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static no.nav.common.utils.NaisUtils.getCredentials;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_PASSWORD_PROPERTY;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_USERNAME_PROPERTY;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.*;

@Configuration
@Import({DatabaseConfig.class})
public class ElasticConfig {


    static final Credentials vaultCredentials = getCredentials("vault", "VEILARBELASTIC_USERNAME", "VEILARBELASTIC_PASSWORD");
    public static final long FORVENTET_MINIMUM_ANTALL_DOKUMENTER = 200_000;
    //public static String VEILARBELASTIC_USERNAME = getRequiredProperty(ELASTICSEARCH_USERNAME_PROPERTY);
    //public static String VEILARBELASTIC_PASSWORD = getRequiredProperty(ELASTICSEARCH_PASSWORD_PROPERTY);

    private static ElasticClientConfig defaultConfig = ElasticClientConfig.builder()
            .username(vaultCredentials.username)
            .password(vaultCredentials.password)
            .hostname(getElasticHostname())
            .port(getElasticPort())
            .scheme(getElasticScheme())
            .build();


    @Bean
    public static RestHighLevelClient restHighLevelClient() {
        return createClient(defaultConfig);
    }

    public static HealthCheckResult checkHealth() {
        long antallDokumenter = ElasticUtils.getCount();
        if (antallDokumenter < FORVENTET_MINIMUM_ANTALL_DOKUMENTER) {
            String feilmelding = String.format("Antall dokumenter i elastic (%s) er mindre enn forventet antall (%s)", antallDokumenter, FORVENTET_MINIMUM_ANTALL_DOKUMENTER);
            HealthCheckResult.unhealthy("Feil mot elastic search", new RuntimeException(feilmelding));
        }
        return HealthCheckResult.healthy();
    }

    @Bean
<<<<<<< HEAD
    public ElasticIndexer elasticIndexer(AktivitetDAO aktivitetDAO, BrukerRepository brukerRepository, PepClient pepClient, VeilarbVeilederClient veilarbVeilederClient, UnleashService unleashService, MetricsClient metricsClient) {
        ElasticService elasticService = new ElasticService(restHighLevelClient(), pepClient, veilarbVeilederClient, unleashService);
        return new ElasticIndexer(aktivitetDAO, brukerRepository, restHighLevelClient(), elasticService, unleashService, metricsClient);
=======
    public ElasticIndexer elasticIndexer(AktivitetDAO aktivitetDAO, BrukerRepository brukerRepository, PepClient pepClient, VeilederService veilederService, UnleashService unleashService) {
        ElasticService elasticService = new ElasticService(restHighLevelClient(), pepClient, veilederService, unleashService);
        return new ElasticIndexer(aktivitetDAO, brukerRepository, restHighLevelClient(), elasticService,unleashService);
>>>>>>> 8e6104206d2c3db1266862aa3a77000ff430366a
    }
}
