package no.nav.pto.veilarbportefolje.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public abstract class KafkaCommonKeyedConsumerService<T> extends KafkaCommonConsumerService<T> {

    @Override
    public void behandleKafkaRecord(ConsumerRecord<String, T> kafkaRecord) {
        loggKafkaRecordInformasjon(kafkaRecord);
        behandleKafkaRecordLogikk(kafkaRecord.value(), kafkaRecord.key());
    }

    protected abstract void behandleKafkaRecordLogikk(T kafkaRecordValue, String kafkaKey);
}
