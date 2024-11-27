package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
public abstract class KafkaCommonConsumerService<T> implements IKafkaCommonConsumerService<T> {
    public void behandleKafkaRecord(ConsumerRecord<String, T> kafkaMelding) {
        secureLog.info(
                "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.partition(),
                kafkaMelding.topic()
        );
        behandleKafkaMeldingLogikk(kafkaMelding.value());
    }

    protected abstract void behandleKafkaMeldingLogikk(T kafkaMelding);
}
