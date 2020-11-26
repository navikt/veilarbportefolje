package no.nav.pto.veilarbportefolje.elastic;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.pto.veilarbportefolje.config.DatabaseConfig;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticIndex;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.function.Supplier;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_PASSWORD_PROPERTY;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_USERNAME_PROPERTY;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.createClient;

@Configuration
@Import({DatabaseConfig.class})
public class ElasticConfig {

    public static final String VEILARBELASTIC_USERNAME = getRequiredProperty(ELASTICSEARCH_USERNAME_PROPERTY);
    public static final String VEILARBELASTIC_PASSWORD = getRequiredProperty(ELASTICSEARCH_PASSWORD_PROPERTY);

    public static final String NAIS_TPA_CLIENT = "NAIS_TPA_CLIENT";
    public static final String NAIS_TPA_CLIENT_CONFIG = "NAIS_TPA_CLIENT_CONFIG";

    public static final  String PTO_ELASTICSEARCH_CLIENT = "PTO_ELASTICSEARCH_CLIENT";
    public static final String PTO_ELASTICSEARCH_CLIENT_CONFIG = "PTO_ELASTICSEARCH_CLIENT_CONFIG";

    public static final String BRUKERINDEKS_ALIAS = "brukerindeks";
    public static final int ELASTICSEARCH_PORT = 9200;

    public static final String NAIS_INTERNAL_SERVICE_URL = "tpa-veilarbelastic-elasticsearch.tpa.svc.nais.local";
    public static final String PTO_ELASTICSEARCH_SERVICE_URL = "pto-portefolje-opendistro-elasticsearch.pto.svc.nais.local";

    public static final String USE_PTO_ELASTICSEARCH_TOGGLE = "veilarbportefolje.use-pto-elasticsearch";

    @Bean
    public ElasticIndex elasticIndex() {
        return ElasticIndex.of(BRUKERINDEKS_ALIAS);
    }

    @Bean
    public Supplier<RestHighLevelClient> restHighLevelClientSupplier(
            @Qualifier(NAIS_TPA_CLIENT) RestHighLevelClient naisTpaRestHighLevelClient,
            @Qualifier(PTO_ELASTICSEARCH_CLIENT) RestHighLevelClient ptoElasticsearchRestHighLevelClient,
            UnleashService unleashService) {

        return () -> unleashService.isEnabled(USE_PTO_ELASTICSEARCH_TOGGLE)
                ? ptoElasticsearchRestHighLevelClient
                : naisTpaRestHighLevelClient;
    }

    @Primary
    @Bean(NAIS_TPA_CLIENT_CONFIG)
    public ElasticClientConfig naisTpaClientConfig() {
        return ElasticClientConfig.builder()
                .username(VEILARBELASTIC_USERNAME)
                .password(VEILARBELASTIC_PASSWORD)
                .hostname(NAIS_INTERNAL_SERVICE_URL)
                .port(ELASTICSEARCH_PORT)
                .scheme("http")
                .build();
    }

    @Bean(NAIS_TPA_CLIENT)
    public RestHighLevelClient naisTpaRestHighLevelClient(@Qualifier(NAIS_TPA_CLIENT_CONFIG) ElasticClientConfig config) {
        return createClient(config);
    }

    @Bean(PTO_ELASTICSEARCH_CLIENT_CONFIG)
    public ElasticClientConfig ptoElasticsearchClientConfig() {
        return ElasticClientConfig.builder()
                .username(VEILARBELASTIC_USERNAME)
                .password(VEILARBELASTIC_PASSWORD)
                .hostname(PTO_ELASTICSEARCH_SERVICE_URL)
                .port(ELASTICSEARCH_PORT)
                .scheme("https")
                .build();
    }

    @Bean(PTO_ELASTICSEARCH_CLIENT)
    public RestHighLevelClient ptoElasticSearchRestHighLevelClient(@Qualifier(PTO_ELASTICSEARCH_CLIENT_CONFIG) ElasticClientConfig config) {
        return createClient(config);
    }

}
