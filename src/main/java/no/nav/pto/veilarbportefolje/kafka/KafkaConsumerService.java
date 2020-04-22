package no.nav.pto.veilarbportefolje.kafka;

public interface KafkaConsumerService {

    void behandleKafkaMelding(String kafkaMelding);
}
