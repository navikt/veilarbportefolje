package no.nav.pto.veilarbportefolje.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface IKafkaCommonConsumerService<T> {
    void behandleKafkaRecord(ConsumerRecord<String, T> kafkaMelding);
}
