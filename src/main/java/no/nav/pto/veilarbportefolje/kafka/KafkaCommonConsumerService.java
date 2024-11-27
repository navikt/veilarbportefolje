package no.nav.pto.veilarbportefolje.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

public abstract class KafkaCommonConsumerService<T> {
    abstract void behandleKafkaRecord(ConsumerRecord<String, T> kafkaRecord);

    void loggKafkaRecordInformasjon(ConsumerRecord<String, T> kafkaRecord) {
        secureLog.info(
                "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
                kafkaRecord.key(),
                kafkaRecord.offset(),
                kafkaRecord.partition(),
                kafkaRecord.topic()
        );
    }
}
