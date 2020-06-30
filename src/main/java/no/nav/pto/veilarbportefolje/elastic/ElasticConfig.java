package no.nav.pto.veilarbportefolje.elastic;

import no.nav.pto.veilarbportefolje.abac.PepClient;
import no.nav.pto.veilarbportefolje.config.DatabaseConfig;
import no.nav.pto.veilarbportefolje.config.ServiceConfig;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetDAO;
import no.nav.pto.veilarbportefolje.service.VeilederService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_PASSWORD_PROPERTY;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_USERNAME_PROPERTY;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.*;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
@Import({
        DatabaseConfig.class,
        ServiceConfig.class
})
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

    @Bean
    public ElasticSelftest elasticSearchHelsesjekk() {
        return new ElasticSelftest(restHighLevelClient());
    }

    @Bean
    public ElasticIndexer elasticIndexer(AktivitetDAO aktivitetDAO, BrukerRepository brukerRepository, PepClient pepClient, VeilederService veilederService, UnleashService unleashService) {
        ElasticService elasticService = new ElasticService(restHighLevelClient(), pepClient, veilederService, unleashService);
        return new ElasticIndexer(aktivitetDAO, brukerRepository, restHighLevelClient(), elasticService,unleashService);
    }
}
