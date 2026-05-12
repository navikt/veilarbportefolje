package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import no.nav.common.utils.EnvironmentUtils.getRequiredProperty
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.KafkaException
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ConsumerRecordRecoverer
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.RetryListener
import org.springframework.util.backoff.ExponentialBackOff

/**
 * Setter opp Spring Kafka for batch-konsumering av aktivitet-meldinger.
 *
 * Nøkkelvalg:
 * - concurrency=1: Én tråd prosesserer alle partisjoner sekvensielt → garantert rekkefølge.
 * - batchListener=true: Spring samler opp meldinger fra én poll() og sender dem som en liste.
 * - AckMode.BATCH: Spring committer offsets automatisk etter at listener-metoden returnerer uten feil.
 * - autoStartup=false: Consumeren starter ikke automatisk — styres av Unleash-toggle i PortefoljeAktivitetKafkaConsumer.
 * - DefaultErrorHandler med ExponentialBackOff: Ved feil vert batchen forsøkt på nytt med aukande
 *   ventetid (0.5s → 1s → 2s → ... maks 60s). Etter 15 minutt utan suksess stoppar containeren.
 *   Offsets vert ALDRI committa for feila meldingar — dei vert konsumerte på nytt ved restart.
 */
@Configuration
@EnableKafka
class PortefoljeAktivitetKafkaConfig {

    @Bean
    fun portefoljeAktivitetConsumerFactory(): ConsumerFactory<String, PortefoljeAktivitetKafkaMelding> {
        val credstorePassword = getRequiredProperty("KAFKA_CREDSTORE_PASSWORD")

        val props = mapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to getRequiredProperty("KAFKA_BROKERS"),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 200,
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to "SSL",
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to getRequiredProperty("KAFKA_TRUSTSTORE_PATH"),
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to credstorePassword,
            SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to getRequiredProperty("KAFKA_KEYSTORE_PATH"),
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to credstorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to credstorePassword,
        )

        return DefaultKafkaConsumerFactory(
            props,
            StringDeserializer(),
            Deserializers.jsonDeserializer(PortefoljeAktivitetKafkaMelding::class.java),
        )
    }

    @Bean
    fun portefoljeAktivitetKafkaListenerContainerFactory(
        portefoljeAktivitetConsumerFactory: ConsumerFactory<String, PortefoljeAktivitetKafkaMelding>,
    ): ConcurrentKafkaListenerContainerFactory<String, PortefoljeAktivitetKafkaMelding> =
        ConcurrentKafkaListenerContainerFactory<String, PortefoljeAktivitetKafkaMelding>().apply {
            setConsumerFactory(portefoljeAktivitetConsumerFactory)
            setConcurrency(1)
            setBatchListener(true)
            containerProperties.ackMode = ContainerProperties.AckMode.BATCH
            setAutoStartup(false)
            setCommonErrorHandler(batchErrorHandler())
        }

    /**
     * Retry med exponential back-off → stopp container.
     *
     * Flyt ved vedvarande feil:
     * 1. Batch feilar → vent 0.5s → retry
     * 2. Feilar igjen → vent 1s → retry
     * 3. Feilar igjen → vent 2s → retry → ... (maks 60s mellom forsøk)
     * 4. Etter 15 minutt totalt → recoverer kastar exception → containeren stoppar
     * 5. Offsets er IKKJE committa
     * 6. sjekkToggle() kan restarte containeren (meldingane vert konsumerte på nytt)
     */
    private fun batchErrorHandler(): DefaultErrorHandler {
        val backOff = ExponentialBackOff().apply {
            initialInterval = 500L
            multiplier = 2.0
            maxInterval = 60_000L
            maxElapsedTime = 15 * 60 * 1_000L
        }

        // Når alle retry-forsøk er brukte opp: kast exception slik at containeren stoppar.
        // Dette sikrar at offsets ALDRI vert committa for feila meldingar.
        val stopContainerRecoverer = ConsumerRecordRecoverer { _, exception ->
            log.error(
                "Alle retry-forsøk brukte opp — stoppar container. Offsets er ikkje committa. Feiltype: {}",
                exception.javaClass.name
            )
            throw exception
        }

        return DefaultErrorHandler(stopContainerRecoverer, backOff).apply {
            setLogLevel(KafkaException.Level.ERROR)
            setRetryListeners(RetryLogger())
        }
    }

    /**
     * Loggar kvart retry-forsøk slik at det er synleg i loggane kva som skjer.
     */
    private class RetryLogger : RetryListener {
        override fun failedDelivery(
            record: ConsumerRecord<*, *>,
            ex: Exception?,
            deliveryAttempt: Int
        ) {
            log.warn("Retry-forsøk {} feilar. Feiltype: {}", deliveryAttempt, ex?.javaClass?.name)
        }
    }

    private companion object {
        private val log = LoggerFactory.getLogger(PortefoljeAktivitetKafkaConfig::class.java)
    }
}
