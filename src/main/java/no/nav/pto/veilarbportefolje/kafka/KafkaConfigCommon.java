package no.nav.pto.veilarbportefolje.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import no.nav.common.kafka.consumer.KafkaConsumerClient;
import no.nav.common.kafka.consumer.TopicConsumer;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRecordProcessor;
import no.nav.common.kafka.consumer.feilhandtering.KafkaConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.PostgresConsumerRepository;
import no.nav.common.kafka.consumer.feilhandtering.StoredRecordConsumer;
import no.nav.common.kafka.consumer.feilhandtering.util.KafkaConsumerRecordProcessorBuilder;
import no.nav.common.kafka.consumer.util.ConsumerUtils;
import no.nav.common.kafka.consumer.util.KafkaConsumerClientBuilder;
import no.nav.pto.veilarbportefolje.cv.CVServiceFromAiven;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.jsonConsumer;
import static org.apache.kafka.clients.CommonClientConfigs.CLIENT_ID_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;

@Configuration
@EnableConfigurationProperties({KafkaAivenProperties.class})
public class KafkaConfigCommon {
    public final static String CONSUMER_GROUP_ID = "veilarbportefolje-consumer";
    public final static String CV_TOPIC = "teampam.samtykke-status-1";

    @Autowired
    KafkaConsumerClient<String, String> consumerClient;

    @Autowired
    KafkaConsumerRecordProcessor consumerRecordProcessor;

    @Bean
    public LockProvider lockProvider(JdbcTemplate jdbcTemplate) {
        return new JdbcTemplateLockProvider(jdbcTemplate);
    }

    @Bean
    public KafkaConsumerRepository kafkaConsumerRepository(DataSource dataSource) {
        return new PostgresConsumerRepository(dataSource);
    }

    @Bean
    public KafkaConsumerRecordProcessor consumerRecordProcessor(
            LockProvider lockProvider,
            KafkaConsumerRepository kafkaConsumerRepository,
            Map<String, TopicConsumer<String, String>> topicConsumers
    ) {
        Map<String, StoredRecordConsumer> storedRecordConsumers = ConsumerUtils.toStoredRecordConsumerMap(
                topicConsumers,
                new StringDeserializer(),
                new StringDeserializer()
        );

        return KafkaConsumerRecordProcessorBuilder
                .builder()
                .withLockProvider(lockProvider)
                .withKafkaConsumerRepository(kafkaConsumerRepository)
                .withRecordConsumers(storedRecordConsumers)
                .build();
    }

    @Bean
    public Map<String, TopicConsumer<String, String>> topicConsumers(
            CVServiceFromAiven cvService
    ) {
        return Map.of(
                CV_TOPIC,
                jsonConsumer(CVMelding.class, cvService::behandleKafkaMelding)
        );
    }

    @Bean
    public Properties kafkaAivenProperties(KafkaAivenProperties kafkaProperties) {
        Properties props = new Properties();

        props.put(CLIENT_ID_CONFIG, CONSUMER_GROUP_ID);
        props.put(SECURITY_PROTOCOL_CONFIG, kafkaProperties.getSecurityProtocol());
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getKafkaBrokers());


        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaProperties.getKafkaCredstorePass());
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, kafkaProperties.getKafkaCredstorePass());
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, kafkaProperties.getKafkaTrustorePath());
        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, kafkaProperties.getKafkaKeystorePath());

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 500000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP_ID);

        return props;
    }

    @Bean
    public KafkaConsumerClient<String, String> consumerClient(
            Map<String, TopicConsumer<String, String>> topicConsumers,
            KafkaConsumerRepository kafkaConsumerRepository,
            Properties kafkaAivenProperties,
            MeterRegistry meterRegistry
    ) {
        return KafkaConsumerClientBuilder.<String, String>builder()
                .withProps(kafkaAivenProperties)
                .withRepository(kafkaConsumerRepository)
                .withSerializers(new StringSerializer(), new StringSerializer())
                .withStoreOnFailureConsumers(topicConsumers)
                .withMetrics(meterRegistry)
                .withLogging()
                .build();
    }

    @PostConstruct
    public void start() {
        consumerRecordProcessor.start();
        consumerClient.start();
    }
}
