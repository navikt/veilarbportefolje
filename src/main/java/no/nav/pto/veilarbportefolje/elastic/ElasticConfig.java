package no.nav.pto.veilarbportefolje.elastic;

import no.nav.pto.veilarbportefolje.config.DatabaseConfig;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticIndex;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_PASSWORD_PROPERTY;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.ELASTICSEARCH_USERNAME_PROPERTY;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.createClient;

@Configuration
@Import({DatabaseConfig.class})
public class ElasticConfig {

    public static final String VEILARBELASTIC_USERNAME = getRequiredProperty(ELASTICSEARCH_USERNAME_PROPERTY);
    public static final String VEILARBELASTIC_PASSWORD = getRequiredProperty(ELASTICSEARCH_PASSWORD_PROPERTY);

    public static final String BRUKERINDEKS_ALIAS = "brukerindeks";
    public static final int ELASTICSEARCH_PORT = 9200;

    public static final String PTO_ELASTICSEARCH_SERVICE_URL = "pto-portefolje-opendistro-elasticsearch.pto.svc.nais.local";

    @Bean
    public ElasticIndex elasticIndex() {
        return ElasticIndex.of(BRUKERINDEKS_ALIAS);
    }

    @Bean
    public ElasticClientConfig elasticsearchClientConfig() {
        return ElasticClientConfig.builder()
                .username(VEILARBELASTIC_USERNAME)
                .password(VEILARBELASTIC_PASSWORD)
                .hostname(PTO_ELASTICSEARCH_SERVICE_URL)
                .port(ELASTICSEARCH_PORT)
                .scheme("https")
                .build();
    }

    @Bean
    public RestHighLevelClient restHighLevelClient(ElasticClientConfig config) {
        return createClient(config);
    }

}
