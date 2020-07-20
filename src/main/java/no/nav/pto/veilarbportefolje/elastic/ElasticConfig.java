package no.nav.pto.veilarbportefolje.elastic;
import no.nav.common.abac.Pep;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.database.DatabaseConfig;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.utils.Credentials;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import no.nav.pto.veilarbportefolje.auth.PepClient;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_PASSWORD_PROPERTY;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_USERNAME_PROPERTY;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.*;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@Import({DatabaseConfig.class})
public class ElasticConfig {

    public static String VEILARBELASTIC_USERNAME = getRequiredProperty(ELASTICSEARCH_USERNAME_PROPERTY);
    public static String VEILARBELASTIC_PASSWORD = getRequiredProperty(ELASTICSEARCH_PASSWORD_PROPERTY);

    private static ElasticClientConfig defaultConfig = ElasticClientConfig.builder()
            .username(VEILARBELASTIC_USERNAME)
            .password(VEILARBELASTIC_PASSWORD)
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


    public ElasticIndexer elasticIndexer(AktivitetDAO aktivitetDAO, BrukerRepository brukerRepository, VeilarbVeilederClient veilarbVeilederClient, UnleashService unleashService, MetricsClient metricsClient, CvService cvService) {
        ElasticService elasticService = new ElasticService(restHighLevelClient(), veilarbVeilederClient, unleashService);
        return new ElasticIndexer(aktivitetDAO, brukerRepository, restHighLevelClient(), elasticService, unleashService, metricsClient, cvService);
}