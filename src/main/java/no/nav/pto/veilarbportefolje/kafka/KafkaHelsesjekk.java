package no.nav.pto.veilarbportefolje.kafka;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import static java.time.Duration.ofSeconds;
import static no.nav.pto.veilarbportefolje.util.KafkaProperties.KAFKA_BROKERS;
import static no.nav.pto.veilarbportefolje.util.KafkaProperties.kafkaProperties;

public class KafkaHelsesjekk implements HealthCheck {

    private final KafkaConsumer<String, String> consumer;
    private final String topic;

    public KafkaHelsesjekk(KafkaConfig.Topic topic) {
        this.consumer = new KafkaConsumer<>(kafkaProperties());
        this.topic = topic.topic;
    }

    public String getMetadata() {
        //return new HelsesjekkMetadata(topic, KAFKA_BROKERS, "Sjekker at vi får kontakt med partisjonene for " + topic, false);
        return null;
    }

    @Override
    public HealthCheckResult checkHealth() {
        try{

            this.consumer.partitionsFor(topic, ofSeconds(10L));
            return HealthCheckResult.healthy();
        }
        catch (Exception e) {
            return HealthCheckResult.unhealthy("Helsesjekken mot kafka feiler : {}", e);
        }

    }
}
