package no.nav.pto.veilarbportefolje.opensearch;

import no.nav.pto.veilarbportefolje.config.DatabaseConfig;
import no.nav.pto.veilarbportefolje.config.EnvironmentProperties;
import no.nav.pto.veilarbportefolje.opensearch.domene.OpensearchClientConfig;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.MalformedURLException;
import java.net.URL;

import static no.nav.pto.veilarbportefolje.opensearch.OpensearchUtils.createClient;

@Configuration
@Import({DatabaseConfig.class})
public class OpensearchConfig {
    public static final String BRUKERINDEKS_ALIAS = "brukerindeks";

    @Bean
    public IndexName opensearchIndex() {
        return new IndexName(BRUKERINDEKS_ALIAS);
    }

    @Bean
    public OpensearchClientConfig opensearchClientConfig(EnvironmentProperties environmentProperties) throws MalformedURLException {
        URL opensearchUrl = new URL(environmentProperties.getOpensearchUri());
        return OpensearchClientConfig.builder()
                .username(environmentProperties.getOpensearchUsername())
                .password(environmentProperties.getOpensearchPassword())
                .hostname(opensearchUrl.getHost())
                .port(opensearchUrl.getPort())
                .scheme(opensearchUrl.getProtocol())
                .build();
    }

    @Bean
    public RestHighLevelClient restHighLevelClient(OpensearchClientConfig config) {
        return createClient(config);
    }

}
