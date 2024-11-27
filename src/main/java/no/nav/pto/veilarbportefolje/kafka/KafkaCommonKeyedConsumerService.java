package no.nav.pto.veilarbportefolje.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

public abstract class KafkaCommonKeyedConsumerService<T> implements IKafkaCommonConsumerService<T> {

    @Override
    public void behandleKafkaRecord(ConsumerRecord<String, T> kafkaMelding) {
        secureLog.info(
                "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.partition(),
                kafkaMelding.topic()
        );
        behandleKafkaMeldingLogikk(kafkaMelding.value(), kafkaMelding.key());
    }

    protected abstract void behandleKafkaMeldingLogikk(T kafkaMelding, String nokkel);
}
