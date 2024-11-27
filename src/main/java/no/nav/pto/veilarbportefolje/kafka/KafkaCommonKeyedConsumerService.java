package no.nav.pto.veilarbportefolje.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public abstract class KafkaCommonKeyedConsumerService<T> extends KafkaCommonConsumerService<T> {

    @Override
    public void behandleKafkaRecord(ConsumerRecord<String, T> kafkaMelding) {
        loggKafkaMeldingInformasjon(kafkaMelding);
        behandleKafkaMeldingLogikk(kafkaMelding.value(), kafkaMelding.key());
    }

    protected abstract void behandleKafkaMeldingLogikk(T kafkaMelding, String nokkel);
}
