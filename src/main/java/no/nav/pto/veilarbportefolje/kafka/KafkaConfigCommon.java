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
import no.nav.pto.veilarbportefolje.cv.CVServiceFromAiven;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import no.nav.pto.veilarbportefolje.elastic.MetricsReporter;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.List;
import java.util.Properties;

import static no.nav.common.kafka.consumer.util.ConsumerUtils.findConsumerConfigsWithStoreOnFailure;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;

@Configuration
@EnableConfigurationProperties({KafkaAivenProperties.class})
public class KafkaConfigCommon {
    public final static String CLIENT_ID_CONFIG = "veilarbportefolje-consumer";
    public final static String GROUP_ID_CONFIG = "veilarbportefolje-consumer";
    public final static String CV_TOPIC = "teampam.samtykke-status-1";

    private final KafkaConsumerClient consumerClient;
    private final KafkaConsumerRecordProcessor consumerRecordProcessor;

    @Bean
    public Properties kafkaAivenProperties(KafkaAivenProperties kafkaProperties) {
        Properties props = new Properties();

        props.put(CLIENT_ID_CONFIG, CLIENT_ID_CONFIG);
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
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID_CONFIG);

        return props;
    }

    public KafkaConfigCommon(CVServiceFromAiven cvServiceFromAiven, Properties kafkaAivenProperties, @Qualifier("Postgres") DataSource dataSource, @Qualifier("PostgresJdbc") JdbcTemplate jdbcTemplate) {
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
                                cvServiceFromAiven::behandleKafkaMelding
                        ));

        consumerClient = KafkaConsumerClientBuilder.builder()
                .withProperties(kafkaAivenProperties)
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
