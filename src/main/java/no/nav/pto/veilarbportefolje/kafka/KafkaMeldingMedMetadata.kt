package no.nav.pto.veilarbportefolje.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord

data class KafkaMeldingMedMetadata<T>(
    val value: T,
    val metadata: KafkaRecordMetadata,
)

data class KafkaRecordMetadata(
    val partition: Int,
    val offset: Long,
    val key: String?,
)

fun <T> ConsumerRecord<String, T>.tilKafkaMeldingMedMetadata(): KafkaMeldingMedMetadata<T> {
    val kafkaValue = requireNotNull(value()) {
        "Kafka-melding mangler value for key=${key()} på topic ${topic()}"
    }

    return KafkaMeldingMedMetadata(
        value = kafkaValue,
        metadata = KafkaRecordMetadata(
            partition = partition(),
            offset = offset(),
            key = key(),
        ),
    )
}
