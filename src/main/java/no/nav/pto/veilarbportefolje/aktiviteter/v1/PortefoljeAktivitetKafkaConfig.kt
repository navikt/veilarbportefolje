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
import java.time.Duration

/**
 * Nøkkelvalg:
 * - concurrency=1: Garanterer rekkefølge — éin tråd prosesserer alle partisjonar sekvensielt.
 * - autoStartup=false: Consumeren styres av Unleash-toggle i [PortefoljeAktivitetKafkaConsumer].
 * - DefaultErrorHandler med ExponentialBackOff: Ved vedvarande feil, retry med aukande ventetid
 *   (0.5s → 1s → 2s → ... maks 10 minutt) heilt til batchen lykkast.
 *   Offsets vert ALDRI committa for feila meldingar.
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
     * Ved vedvarande feil: retry med capped exponential back-off utan å stoppe containeren.
     * Då kan ikkje feature-toggle-sjekken restarte containeren og starte ein ny retry-syklus.
     */
    private fun batchErrorHandler(): DefaultErrorHandler {
        val backOff = ExponentialBackOff().apply {
            initialInterval = HALVT_SEKUND
            multiplier = DOBLING
            maxInterval = TI_MINUTT
            maxElapsedTime = UENDELEG_TID
        }

        // Skal i praksis ikkje kallast med Long.MAX_VALUE, men hindrar offset-skip dersom back-off stoppar.
        val failIfBackOffStopsRecoverer = ConsumerRecordRecoverer { _, exception ->
            log.error(
                "Back-off stoppa uventa. Offsets er ikkje committa. Feiltype: {}",
                exception.javaClass.name
            )
            throw exception
        }

        return DefaultErrorHandler(failIfBackOffStopsRecoverer, backOff).apply {
            setLogLevel(KafkaException.Level.DEBUG)
            setRetryListeners(RetryLogger())
        }
    }

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
        private val HALVT_SEKUND = Duration.ofMillis(500).toMillis()
        private const val DOBLING = 2.0
        private val TI_MINUTT = Duration.ofMinutes(10).toMillis()
        private const val UENDELEG_TID = Long.MAX_VALUE
    }
}
