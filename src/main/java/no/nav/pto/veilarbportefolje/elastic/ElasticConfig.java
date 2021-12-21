package no.nav.pto.veilarbportefolje.elastic;

import no.nav.pto.veilarbportefolje.config.DatabaseConfig;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticClientConfig;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.MalformedURLException;
import java.net.URL;

import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.createClient;

@Configuration
@Import({DatabaseConfig.class})
public class ElasticConfig {
    public static final String BRUKERINDEKS_ALIAS = "brukerindeks";

    @Bean
    public IndexName elasticIndex() {
        return new IndexName(BRUKERINDEKS_ALIAS);
    }

    @Bean
    public ElasticClientConfig elasticsearchClientConfig(EnvironmentProperties environmentProperties) throws MalformedURLException {
        URL elasticUrl = new URL(environmentProperties.getElasticUri());
        return ElasticClientConfig.builder()
                .username(environmentProperties.getElasticUsername())
                .password(environmentProperties.getElasticPassword())
                .hostname(elasticUrl.getHost())
                .port(elasticUrl.getPort())
                .scheme("https")
                .build();
    }

    @Bean
    public RestHighLevelClient restHighLevelClient(ElasticClientConfig config) {
        return createClient(config);
    }

}
