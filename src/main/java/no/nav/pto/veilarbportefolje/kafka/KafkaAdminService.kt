package no.nav.pto.veilarbportefolje.kafka

import no.nav.common.kafka.util.KafkaPropertiesPreset.aivenDefaultConsumerProperties
import no.nav.pto.veilarbportefolje.admin.dto.KafkaPartitionOffsetDto
import no.nav.pto.veilarbportefolje.admin.dto.KafkaResetOffsetRequest
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.GroupNotEmptyException
import org.apache.kafka.common.errors.RebalanceInProgressException
import org.apache.kafka.common.errors.UnknownMemberIdException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

@Service
class KafkaAdminService(private val adminClientSupplier: Supplier<AdminClient>) {
    constructor() : this(Supplier { AdminClient.create(aivenDefaultConsumerProperties(KafkaConfigCommon.CLIENT_ID_CONFIG)) })

    companion object {
        val CONSUMER_GROUPS = listOf(
            KafkaConfigCommon.CLIENT_ID_CONFIG,
            KafkaConfigCommon.CV_CLIENT_ID_CONFIG,
            KafkaConfigCommon.ARBEIDSSOEKER_V3_CLIENT_ID_CONFIG
        )
        const val ADMIN_CLIENT_TIMEOUT_SECONDS = 10L
        private const val MAX_PARTITIONS_IN_REQUEST = 100
    }

    fun hentKonsumerteTopics(): List<String> = KafkaConfigCommon.Topic.entries.map { it.topicName }

    fun hentCommitedeOffsets(topic: String): List<KafkaPartitionOffsetDto> {
        if (topic !in hentKonsumerteTopics()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ukjent topic: $topic")
        }

        return adminClientSupplier.get().use { adminClient ->
            CONSUMER_GROUPS.flatMap { group ->
                adminClient.listConsumerGroupOffsets(group)
                    .partitionsToOffsetAndMetadata()
                    .get(ADMIN_CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .entries
                    .filter { (topicPartition, offsetAndMetadata) ->
                        topicPartition.topic() == topic && offsetAndMetadata != null
                    }
                    .map { (topicPartition, offsetAndMetadata) ->
                        KafkaPartitionOffsetDto(
                            consumerGroup = group,
                            topic = topicPartition.topic(),
                            partition = topicPartition.partition(),
                            offset = offsetAndMetadata.offset()
                        )
                    }
            }.sortedWith(compareBy<KafkaPartitionOffsetDto> { it.consumerGroup }.thenBy { it.partition })
        }
    }

    fun resetOffsets(request: KafkaResetOffsetRequest, utfortAv: String = "ukjent") {
        if (request.topic !in hentKonsumerteTopics()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ukjent topic: ${request.topic}")
        }
        if (request.consumerGroup !in CONSUMER_GROUPS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ukjent consumer group: ${request.consumerGroup}")
        }
        if (request.partitionOffsets.isEmpty() || request.partitionOffsets.size > MAX_PARTITIONS_IN_REQUEST) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Antall partisjoner må være mellom 1 og $MAX_PARTITIONS_IN_REQUEST")
        }
        if (request.partitionOffsets.any { (partition, offset) -> partition < 0 || offset < 0 }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Partisjon og offset må være >= 0")
        }

        adminClientSupplier.get().use { adminClient ->
            try {
                val eksisterendeOffsets = adminClient.listConsumerGroupOffsets(request.consumerGroup)
                    .partitionsToOffsetAndMetadata()
                    .get(ADMIN_CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

                val nyeOffsets: Map<TopicPartition, OffsetAndMetadata> =
                    request.partitionOffsets.entries.associate { (partition, nyOffset) ->
                        val topicPartition = TopicPartition(request.topic, partition)
                        val gammelOffset = eksisterendeOffsets[topicPartition]?.offset()
                        secureLog.info(
                            "Resetter Kafka-offset for consumerGroup=${request.consumerGroup}, topic=${request.topic}, partition=$partition fra offset=$gammelOffset til offset=$nyOffset (utførtAv=$utfortAv)"
                        )
                        topicPartition to OffsetAndMetadata(nyOffset)
                    }

                adminClient.alterConsumerGroupOffsets(request.consumerGroup, nyeOffsets)
                    .all()
                    .get(ADMIN_CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

                secureLog.info(
                    "Fullførte oppdatering av Kafka-offsets for consumerGroup=${request.consumerGroup}, topic=${request.topic}, partisjoner=${request.partitionOffsets.keys} (utførtAv=$utfortAv)"
                )
            } catch (e: ExecutionException) {
                val cause = e.cause
                if (cause is UnknownMemberIdException ||
                    cause is GroupNotEmptyException ||
                    cause is RebalanceInProgressException ||
                    cause is IllegalStateException
                ) {
                    secureLog.warn(
                        "Kunne ikke resette Kafka-offsets for consumerGroup=${request.consumerGroup} og topic=${request.topic} fordi gruppen har aktive konsumenter (utførtAv=$utfortAv)"
                    )
                    throw ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Kan ikke resette offset mens konsumentgruppen '${request.consumerGroup}' har aktive konsumenter. Skru av konsument-toggle først."
                    )
                }
                secureLog.error(
                    "Feil ved oppdatering av Kafka-offsets for consumerGroup=${request.consumerGroup} og topic=${request.topic} (utførtAv=$utfortAv)",
                    e
                )
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved oppdatering av Kafka-offsets")
            }
        }
    }
}
