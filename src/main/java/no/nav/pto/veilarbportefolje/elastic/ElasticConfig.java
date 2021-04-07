package no.nav.pto.veilarbportefolje.elastic;

import no.nav.common.health.HealthCheckResult;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.config.DatabaseConfig;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringRepository;
import okhttp3.OkHttpClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.common.utils.EnvironmentUtils.isProduction;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_PASSWORD_PROPERTY;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_USERNAME_PROPERTY;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.*;

@Configuration
@Import({DatabaseConfig.class})
public class ElasticConfig {

    public static String VEILARBELASTIC_USERNAME = getRequiredProperty(ELASTICSEARCH_USERNAME_PROPERTY);
    public static String VEILARBELASTIC_PASSWORD = getRequiredProperty(ELASTICSEARCH_PASSWORD_PROPERTY);

    public static final long FORVENTET_MINIMUM_ANTALL_DOKUMENTER = 200_000;

    private static final ElasticClientConfig defaultConfig = ElasticClientConfig.builder()
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

    @Bean
    public ElasticServiceV2 elasticServiceV2(RestHighLevelClient restHighLevelClient) {
        return new ElasticServiceV2(restHighLevelClient, new IndexName(getAlias()));
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
    public ElasticService elasticService(RestHighLevelClient restHighLevelClient, VeilarbVeilederClient veilarbVeilederClient) {
        String baseUrl = (isProduction().orElse(false)) ? "https://app.adeo.no/" : "https://app-q1.dev.adeo.no/";
        return new ElasticService(restHighLevelClient, veilarbVeilederClient, new IndexName(getAlias()), baseUrl);
    }


    @Bean
    public ElasticIndexer elasticIndexer(AktivitetDAO aktivitetDAO, BrukerRepository brukerRepository, RestHighLevelClient restHighLevelClient, SisteEndringRepository sisteEndringRepository) {
        return new ElasticIndexer(aktivitetDAO, brukerRepository, restHighLevelClient, sisteEndringRepository, new IndexName(getAlias()));


    }
}
