package no.nav.pto.veilarbportefolje.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.PostgresConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.pto.veilarbportefolje.cv.CVService;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import no.nav.pto.veilarbportefolje.elastic.MetricsReporter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.List;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure;
import static no.nav.common.kafka.util.KafkaPropertiesPreset.aivenDefaultConsumerProperties;

@Configuration
public class KafkaConfigCommon {
    public final static String CLIENT_ID_CONFIG = "veilarbportefolje-consumer";
    public final static String CV_TOPIC = "teampam.samtykke-status-1";

    private final KafkaConsumerClient consumerClient;
    private final KafkaConsumerRecordProcessor consumerRecordProcessor;

    public KafkaConfigCommon(CVService cvService, @Qualifier("Postgres") DataSource dataSource, @Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplate) {
        KafkaConsumerRepository consumerRepository = new PostgresConsumerRepository(dataSource);
        MeterRegistry prometheusMeterRegistry = new MetricsReporter.ProtectedPrometheusMeterRegistry();

        List<KafkaConsumerClientBuilder.TopicConfig<?, ?>> topicConfigs =
                List.of(new KafkaConsumerClientBuilder.TopicConfig<String, CVMelding>()
                        .withLogging()
                        .withMetrics(prometheusMeterRegistry)
                        .withStoreOnFailure(consumerRepository)
                        .withConsumerConfig(
                                CV_TOPIC,
                                Deserializers.stringDeserializer(),
                                Deserializers.jsonDeserializer(CVMelding.class),
                                cvService::behandleKafkaMeldingCVHjemmel
                        ));

        consumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(aivenDefaultConsumerProperties(CLIENT_ID_CONFIG))
                .withTopicConfigs(topicConfigs)
                .build();

        consumerRecordProcessor = KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(new JdbcTemplateLockProvider(jdbcTemplate))
                .withKafkaConsumerRepository(consumerRepository)
                .withConsumerConfigs(findConsumerConfigsWithStoreOnFailure(topicConfigs))
                .build();

    }


    @PostConstruct
    public void start() {
        consumerRecordProcessor.start();
        consumerClient.start();
    }
}
