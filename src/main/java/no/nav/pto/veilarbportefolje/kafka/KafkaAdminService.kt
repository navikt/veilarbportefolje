package no.nav.pto.veilarbportefolje.kafka

import no.nav.common.kafka.util.KafkaPropertiesPreset.aivenDefaultConsumerProperties
import no.nav.pto.veilarbportefolje.admin.dto.KafkaPartitionOffsetDto
import no.nav.pto.veilarbportefolje.admin.dto.KafkaResetOffsetRequest
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.GroupNotEmptyException
import org.apache.kafka.common.errors.RebalanceInProgressException
import org.apache.kafka.common.errors.UnknownMemberIdException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

@Service
class KafkaAdminService(private val adminClientSupplier: Supplier<AdminClient>) {
    constructor() : this(Supplier { AdminClient.create(aivenDefaultConsumerProperties(KafkaConfigCommon.CLIENT_ID_CONFIG)) })

    private val log = LoggerFactory.getLogger(KafkaAdminService::class.java)

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
                        log.info(
                            "Resetter Kafka-offset for consumerGroup={}, topic={}, partition={} fra offset={} til offset={} (utførtAv={})",
                            request.consumerGroup,
                            request.topic,
                            partition,
                            gammelOffset,
                            nyOffset,
                            utfortAv
                        )
                        topicPartition to OffsetAndMetadata(nyOffset)
                    }

                adminClient.alterConsumerGroupOffsets(request.consumerGroup, nyeOffsets)
                    .all()
                    .get(ADMIN_CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

                log.info(
                    "Fullførte oppdatering av Kafka-offsets for consumerGroup={}, topic={}, partisjoner={} (utførtAv={})",
                    request.consumerGroup,
                    request.topic,
                    request.partitionOffsets.keys,
                    utfortAv
                )
            } catch (e: ExecutionException) {
                val cause = e.cause
                if (cause is UnknownMemberIdException ||
                    cause is GroupNotEmptyException ||
                    cause is RebalanceInProgressException ||
                    cause is IllegalStateException
                ) {
                    log.warn(
                        "Kunne ikke resette Kafka-offsets for consumerGroup={} og topic={} fordi gruppen har aktive konsumenter (utførtAv={})",
                        request.consumerGroup,
                        request.topic,
                        utfortAv
                    )
                    throw ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Kan ikke resette offset mens konsumentgruppen '${request.consumerGroup}' har aktive konsumenter. Skru av konsument-toggle først."
                    )
                }
                log.error(
                    "Feil ved oppdatering av Kafka-offsets for consumerGroup={} og topic={} (utførtAv={})",
                    request.consumerGroup,
                    request.topic,
                    utfortAv,
                    e
                )
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Feil ved oppdatering av Kafka-offsets")
            }
        }
    }
}
