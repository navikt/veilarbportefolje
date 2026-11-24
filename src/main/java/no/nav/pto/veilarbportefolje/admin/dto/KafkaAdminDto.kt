package no.nav.pto.veilarbportefolje.admin.dto

data class KafkaPartitionOffsetDto(
    val consumerGroup: String,
    val topic: String,
    val partition: Int,
    val offset: Long
)

data class KafkaResetOffsetRequest(
    val consumerGroup: String,
    val topic: String,
    val partitionOffsets: Map<Int, Long>
)
