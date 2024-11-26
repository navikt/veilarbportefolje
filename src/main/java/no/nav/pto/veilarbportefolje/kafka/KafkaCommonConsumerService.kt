package no.nav.pto.veilarbportefolje.kafka

import lombok.extern.slf4j.Slf4j
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.apache.kafka.clients.consumer.ConsumerRecord

interface KafkaCommonConsumerService<T> {
    fun behandleKafkaRecord(kafkaMelding: ConsumerRecord<String?, T>)
}

@Slf4j
abstract class KafkaCommonNonKeyedConsumerService<T> : KafkaCommonConsumerService<T> {
    override fun behandleKafkaRecord(kafkaMelding: ConsumerRecord<String?, T>) {
        loggKafkaMeldingInfo(kafkaMelding)
        behandleKafkaMeldingLogikk(kafkaMelding.value())
    }

    protected abstract fun behandleKafkaMeldingLogikk(kafkaMelding: T)
}

@Slf4j
abstract class KafkaCommonKeyedConsumerService<T> : KafkaCommonConsumerService<T> {
    override fun behandleKafkaRecord(kafkaMelding: ConsumerRecord<String?, T>) {
        loggKafkaMeldingInfo(kafkaMelding)
        behandleKafkaMeldingLogikk(kafkaMelding.value(), kafkaMelding.key()!!)
    }

    protected abstract fun behandleKafkaMeldingLogikk(kafkaMelding: T, nokkel: String)
}

private fun <T> loggKafkaMeldingInfo(kafkaMelding: ConsumerRecord<String?, T>) {
    secureLog.info(
        "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
        kafkaMelding.key(),
        kafkaMelding.offset(),
        kafkaMelding.partition(),
        kafkaMelding.topic()
    )
}