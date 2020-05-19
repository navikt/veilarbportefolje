package no.nav.pto.veilarbportefolje.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.profilering.ProfileringService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

@Configuration
public class KafkaConfig {

    public final static String KAFKA_OPPFOLGING_TOGGLE = "portefolje.kafka.oppfolging";

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

    // Registreringbruker avro for serializering därför spesialcase för denna consumer
    @Bean
    public KafkaConsumerRunnable kafkaConsumerRegistrering(RegistreringService registreringService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<ArbeidssokerRegistrertEvent>(registreringService, unleashService, KafkaProperties.kafkaMedAvroProperties(), Topic.KAFKA_REGISTRERING_CONSUMER_TOPIC, "veilarbportfolje.registrering");
    }

    @Bean
    public KafkaConsumerRunnable kafkaConsumeProfilering(ProfileringService profileringService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<ArbeidssokerRegistrertEvent>(profileringService, unleashService, KafkaProperties.kafkaMedAvroProperties(), Topic.KAFKA_REGISTRERING_CONSUMER_TOPIC, "veilarbportfolje.registrering");
    }

}
