package no.nav.pto.veilarbportefolje.kafka;

import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import java.util.Properties;

import static java.time.Duration.ofSeconds;
import static no.nav.pto.veilarbportefolje.util.KafkaProperties.KAFKA_BROKERS;
import static no.nav.pto.veilarbportefolje.util.KafkaProperties.kafkaProperties;

public class KafkaHelsesjekk implements Helsesjekk {

    private final KafkaConsumer<String, String> consumer;
    private final String topic;

    public KafkaHelsesjekk(KafkaConfig.Topic topic) {
        Properties properties = kafkaProperties();

        this.consumer = new KafkaConsumer<>(properties);
        this.topic = topic.topic;
    }

    @Override
    public void helsesjekk() {
        this.consumer.partitionsFor(topic, ofSeconds(10L));
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata(topic, KAFKA_BROKERS, "Sjekker at vi får kontakt med partisjonene for " + topic, false);
    }
}
