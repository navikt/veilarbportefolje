package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Slf4j
public abstract class KafkaCommonNonKeyedConsumerService<T> extends KafkaCommonConsumerService<T> {
    public void behandleKafkaRecord(ConsumerRecord<String, T> kafkaMelding) {
        loggKafkaMeldingInformasjon(kafkaMelding);
        behandleKafkaMeldingLogikk(kafkaMelding.value());
    }

    protected abstract void behandleKafkaMeldingLogikk(T kafkaMelding);
}
