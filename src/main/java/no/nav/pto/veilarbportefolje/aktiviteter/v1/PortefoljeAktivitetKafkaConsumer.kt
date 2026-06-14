package no.nav.pto.veilarbportefolje.aktiviteter.v1

import io.getunleash.DefaultUnleash
import no.nav.common.types.identer.AktorId
import no.nav.pto.veilarbportefolje.config.FeatureToggle
import no.nav.pto.veilarbportefolje.kafka.unleash.KafkaAivenUnleash
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

const val PORTEFOLJE_AKTIVITET_CONTAINER_ID = "portefolje-aktivitet-consumer"

/**
 * Batch-consumer for aktivitet-meldinger fra Kafka.
 *
 * Consumeren styres av Unleash-toggles via [sjekkToggle], som køyrer kvart 30. sekund.
 * autoStartup=false i config gjer at consumeren ikkje startar før togglen er på.
 */
@Component
class PortefoljeAktivitetKafkaConsumer(
    private val aktivitetKafkaMeldingService: PortefoljeAktivitetKafkaMeldingService,
    private val registry: KafkaListenerEndpointRegistry,
    private val defaultUnleash: DefaultUnleash,
    private val consumerState: PortefoljeAktivitetKafkaConsumerState,
    private val opensearchIndexer: OpensearchIndexer
) {
    private val kafkaAivenUnleash = KafkaAivenUnleash(defaultUnleash)

    @KafkaListener(
        id = PORTEFOLJE_AKTIVITET_CONTAINER_ID,
        idIsGroup = false,
        groupId = CONSUMER_GROUP_ID,
        topics = [AKTIVITET_TOPIC],
        containerFactory = "portefoljeAktivitetKafkaListenerContainerFactory",
    )
    fun onBatch(records: List<ConsumerRecord<String, PortefoljeAktivitetKafkaMelding>>) {
        log.info(
            "Mottok batch med {} meldinger fra topic {} (partitions: {}).",
            records.size,
            AKTIVITET_TOPIC,
            records.map { it.partition() }.distinct().sorted(),
        )

        aktivitetKafkaMeldingService.behandleKafkaRecords(records)

        val aktoerIder = records.map { AktorId.of(it.value().aktorId) }.toSet()
        opensearchIndexer.indekserBolk(aktoerIder.toList())
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 5_000)
    fun sjekkToggle() {
        val container = registry.getListenerContainer(PORTEFOLJE_AKTIVITET_CONTAINER_ID) ?: return

        if (!skalKonsumere()) {
            consumerState.nullstillFeilstopp()
            if (container.isRunning) {
                log.info("Stopper aktivitet-consumer (toggle er av).")
                container.stop()
            }
            return
        }

        if (consumerState.erStoppetPaaGrunnAvVedvarendeFeil()) {
            return
        }

        if (!container.isRunning) {
            log.info("Starter aktivitet-consumer (toggle er på).")
            container.start()
        }
    }

    private fun skalKonsumere(): Boolean =
        defaultUnleash.isEnabled(FeatureToggle.KAFKA_PORTEFOLJE_AKTIVITET_V1_START) && !kafkaAivenUnleash.get()

    private companion object {
        private val log = LoggerFactory.getLogger(PortefoljeAktivitetKafkaConsumer::class.java)
        private const val CONSUMER_GROUP_ID = "veilarbportefolje-portefolje-aktivitet-consumer"
        const val AKTIVITET_TOPIC = "pto.aktivitet-portefolje-v1"
    }
}
