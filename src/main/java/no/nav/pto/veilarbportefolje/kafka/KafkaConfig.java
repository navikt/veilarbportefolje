package no.nav.pto.veilarbportefolje.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.registrering.KafkaConsumerRegistrering;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

@Configuration
public class KafkaConfig {

    public final static String KAFKA_OPPFOLGING_TOGGLE = "portefolje.kafka.oppfolging";
    public final static String KAFKA_OPPFOLGING_BEHANDLE_MELDINGER_TOGGLE = "portefolje.kafka.oppfolging_behandle_meldinger";

    public enum Topic {
        VEDTAK_STATUS_ENDRING_TOPIC("aapen-oppfolging-vedtakStatusEndring-v1-" + requireEnvironmentName()),
        DIALOG_CONSUMER_TOPIC("aapen-fo-endringPaaDialog-v1-" + requireEnvironmentName()),
        OPPFOLGING_CONSUMER_TOPIC("aapen-fo-endringPaaOppfolgingStatus-v1-" + requireEnvironmentName()),
        KAFKA_REGISTRERING_CONSUMER_TOPIC( "aapen-arbeid-arbeidssoker-registrert-" + requireEnvironmentName());

        final String topic;

        Topic(String topic) {
            this.topic = topic;
        }
    }

    public static List<KafkaHelsesjekk> getHelseSjekker() {
        List<Topic> topics = Arrays.asList(Topic.values());
        return topics.stream().map(topic -> new KafkaHelsesjekk(topic)).collect(toList());
    }

    @Bean
    public Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer() {
        final String KAFKA_SCHEMAS_URL = getRequiredProperty("KAFKA_SCHEMAS_URL");
        HashMap<String, Object> props = KafkaProperties.kafkaProperties();
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, KAFKA_SCHEMAS_URL);

        Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer = new KafkaConsumer<>(props);
        kafkaRegistreringConsumer.subscribe(Collections.singletonList(Topic.KAFKA_REGISTRERING_CONSUMER_TOPIC.topic));
        return kafkaRegistreringConsumer;
    }

    // Registreringbruker avro for serializering därför spesialcase för denna consumer
    @Bean
    public KafkaConsumerRegistrering kafkaConsumerRegistrering(RegistreringService registreringService, Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer, UnleashService unleashService) {
        return new KafkaConsumerRegistrering(registreringService, kafkaRegistreringConsumer, unleashService);
    }

}
