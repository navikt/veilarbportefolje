package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import static no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic.ENDRING_PAA_OPPFOLGINGSBRUKER;

@Slf4j
public abstract class KafkaCommonConsumerService<T> {
    public void behandleKafkaRecord(ConsumerRecord<String, T> kafkaMelding) {
        if(ENDRING_PAA_OPPFOLGINGSBRUKER.getTopicName().equals(kafkaMelding.topic())){
            log.info(
                    "Behandler kafka-melding på offset: {}, og partition: {} på topic {}",
                    kafkaMelding.offset(),
                    kafkaMelding.partition(),
                    kafkaMelding.topic()
            );
            behandleKafkaMeldingLogikk(kafkaMelding.value());
            return;
        }
        log.info(
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
