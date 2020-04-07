package no.nav.pto.veilarbportefolje.vedtakstotte;

import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;

import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;

@Configuration
public class VedtakConfig {
    private static String KAFKA_VEDTAKSTOTTE_CONSUMER_TOPIC = "aapen-oppfolging-vedtakStatusEndring-v1-" + requireEnvironmentName();

    @Bean
    public Consumer<String, String> kafkaVedtakStotteConsumer() {
        Consumer<String, String> kafkaVedtakStotteConsumer = new KafkaConsumer<>(KafkaProperties.kafkaProperties());
        kafkaVedtakStotteConsumer.subscribe(Collections.singletonList(KAFKA_VEDTAKSTOTTE_CONSUMER_TOPIC));
        return  kafkaVedtakStotteConsumer;
    }

    @Bean
    public VedtakStatusRepository vedtakStatusRepository(JdbcTemplate jdbcTemplate) {
        return new VedtakStatusRepository(jdbcTemplate);
    }

    @Bean
    public VedtakService vedtakService(VedtakStatusRepository vedtakStatusRepository, ElasticIndexer elasticIndexer, AktoerService aktoerService) {
        return new VedtakService(vedtakStatusRepository, elasticIndexer, aktoerService);
    }
}
