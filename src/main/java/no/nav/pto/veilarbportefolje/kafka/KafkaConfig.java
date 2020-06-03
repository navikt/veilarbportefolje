package no.nav.pto.veilarbportefolje.kafka;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.registrering.KafkaConsumerRegistrering;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.util.EnvironmentUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.util.KafkaProperties.avroProperties;
import static no.nav.sbl.util.EnvironmentUtils.EnviromentClass.Q;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;

@Configuration
public class KafkaConfig {

    public final static String KAFKA_OPPFOLGING_TOGGLE = "portefolje.kafka.oppfolging";
    public final static String KAFKA_OPPFOLGING_BEHANDLE_MELDINGER_TOGGLE = "portefolje.kafka.oppfolging_behandle_meldinger";

    public enum Topic {
        VEDTAK_STATUS_ENDRING_TOPIC("aapen-oppfolging-vedtakStatusEndring-v1-" + requireEnvironmentName()),
        DIALOG_CONSUMER_TOPIC("aapen-fo-endringPaaDialog-v1-" + requireEnvironmentName()),
        OPPFOLGING_CONSUMER_TOPIC("aapen-fo-endringPaaOppfolgingStatus-v1-" + requireEnvironmentName()),
        KAFKA_REGISTRERING_CONSUMER_TOPIC("aapen-arbeid-arbeidssoker-registrert-" + requireEnvironmentName()),
        KAFKA_AKTIVITER_CONSUMER_TOPIC("aapen-fo-endringPaaAktivitet-v1-" + requireEnvironmentName()),
        CV_ENDRET_TOPIC("arbeid-pam-cv-endret-" + getCvTopicVersion() + "-" + requireEnvironmentName());

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
        Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer = new KafkaConsumer<>(avroProperties());
        kafkaRegistreringConsumer.subscribe(Collections.singletonList(Topic.KAFKA_REGISTRERING_CONSUMER_TOPIC.topic));
        return kafkaRegistreringConsumer;
    }

    // Registreringbruker avro for serializering därför spesialcase för denna consumer
    @Bean
    public KafkaConsumerRegistrering kafkaConsumerRegistrering(RegistreringService registreringService, Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer, UnleashService unleashService) {
        return new KafkaConsumerRegistrering(registreringService, kafkaRegistreringConsumer, unleashService);
    }

    private static String getCvTopicVersion() {
        if (EnvironmentUtils.isEnvironmentClass(Q)) {
            return "v4";
        }else {
            return "v5";
        }
    }
}
