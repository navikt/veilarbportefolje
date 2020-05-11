package no.nav.pto.veilarbportefolje.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService;
import no.nav.pto.veilarbportefolje.registrering.KafkaConsumerRegistrering;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.time.Duration;
import java.util.*;

import static java.time.Duration.ofSeconds;
import static no.nav.pto.veilarbportefolje.util.KafkaProperties.KAFKA_BROKERS;
import static no.nav.pto.veilarbportefolje.util.KafkaProperties.kafkaProperties;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

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

    static {
        List<Topic> topics = Arrays.asList(Topic.values());
        topics.forEach(topic -> new KafkaHelsesjekk(topic));
    }

    @Bean
    public Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer() {
        final String KAFKA_SCHEMAS_URL = getRequiredProperty("KAFKA_SCHEMAS_URL");
        HashMap<String, Object> props = kafkaProperties();
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

    @Bean
    public KafkaConsumerRunnable kafkaDialogConsumer(DialogService dialogService, UnleashService unleashService) {
        return new KafkaConsumerRunnable(dialogService, unleashService, Topic.DIALOG_CONSUMER_TOPIC, Optional.of(("veilarbdialog.kafka")));
    }

    @Bean
    public KafkaConsumerRunnable kafkaVedtakConsumer(VedtakService vedtakService, UnleashService unleashService) {
        return new KafkaConsumerRunnable(vedtakService, unleashService, Topic.VEDTAK_STATUS_ENDRING_TOPIC, Optional.of(("veilarbportfolje-hent-data-fra-vedtakstotte")));
    }

    @Bean
    public KafkaConsumerRunnable kafkaVedtakConsumer(OppfolgingService oppfolgingService, UnleashService unleashService) {
        return new KafkaConsumerRunnable(oppfolgingService, unleashService, Topic.VEDTAK_STATUS_ENDRING_TOPIC, Optional.of(KAFKA_OPPFOLGING_TOGGLE));
    }


}
