package no.nav.pto.veilarbportefolje.kafka

import lombok.extern.slf4j.Slf4j
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.apache.kafka.clients.consumer.ConsumerRecord

@Slf4j
abstract class KafkaCommonConsumerService<T> {
    fun behandleKafkaRecord(kafkaMelding: ConsumerRecord<String?, T>) {
        secureLog.info(
            "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
            kafkaMelding.key(),
            kafkaMelding.offset(),
            kafkaMelding.partition(),
            kafkaMelding.topic()
        )
        behandleKafkaMeldingLogikk(kafkaMelding.value())
    }

    protected abstract fun behandleKafkaMeldingLogikk(kafkaMelding: T)
}