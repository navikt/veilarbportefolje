package no.nav.pto.veilarbportefolje.aktiviteter.v1

import io.getunleash.DefaultUnleash
import no.nav.pto.veilarbportefolje.config.FeatureToggle
import no.nav.pto.veilarbportefolje.kafka.unleash.KafkaAivenUnleash
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Batch-consumer for aktivitet-meldinger fra Kafka.
 *
 * Hvordan dette fungerer:
 * 1. Spring Kafka poller Kafka i bakgrunnen og samler opp meldinger.
 * 2. Når det finnes meldinger, kaller Spring [onBatch] med alle meldinger fra én poll().
 * 3. Vi prosesserer hele batchen i én transaksjon via [PortefoljeAktivitetKafkaMeldingService].
 * 4. Hvis [onBatch] returnerer uten feil, committer Spring offsets automatisk (AckMode.BATCH).
 *    Hvis [onBatch] kaster exception, committer Spring IKKE — meldingene vil bli levert på nytt.
 * 5. [sjekkToggle] kjører hvert 30. sekund og starter/stopper consumeren basert på Unleash-toggles.
 */
@Component
class PortefoljeAktivitetKafkaConsumer(
    private val aktivitetKafkaMeldingService: PortefoljeAktivitetKafkaMeldingService,
    private val registry: KafkaListenerEndpointRegistry,
    private val defaultUnleash: DefaultUnleash,
) {
    private val kafkaAivenUnleash = KafkaAivenUnleash(defaultUnleash)

    @KafkaListener(
        id = CONTAINER_ID,
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
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 5_000)
    fun sjekkToggle() {
        val container = registry.getListenerContainer(CONTAINER_ID) ?: return

        if (skalKonsumere() && !container.isRunning) {
            log.info("Starter aktivitet-consumer (toggle er på).")
            container.start()
        } else if (!skalKonsumere() && container.isRunning) {
            log.info("Stopper aktivitet-consumer (toggle er av).")
            container.stop()
        }
    }

    private fun skalKonsumere(): Boolean =
        defaultUnleash.isEnabled(FeatureToggle.KAFKA_PORTEFOLJE_AKTIVITET_V1_START) && !kafkaAivenUnleash.get()

    private companion object {
        private val log = LoggerFactory.getLogger(PortefoljeAktivitetKafkaConsumer::class.java)
        private const val CONTAINER_ID = "portefolje-aktivitet-consumer"
        const val AKTIVITET_TOPIC = "pto.aktivitet-portefolje-v1"
    }
}
