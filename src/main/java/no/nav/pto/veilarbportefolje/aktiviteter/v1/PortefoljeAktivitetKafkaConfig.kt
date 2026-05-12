package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.common.kafka.consumer.util.deserializer.Deserializers
import no.nav.common.utils.EnvironmentUtils.getRequiredProperty
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties

/**
 * Setter opp Spring Kafka for batch-konsumering av aktivitet-meldinger.
 *
 * Nøkkelvalg:
 * - concurrency=1: Én tråd prosesserer alle partisjoner sekvensielt → garantert rekkefølge.
 * - batchListener=true: Spring samler opp meldinger fra én poll() og sender dem som en liste.
 * - AckMode.BATCH: Spring committer offsets automatisk etter at listener-metoden returnerer uten feil.
 * - autoStartup=false: Consumeren starter ikke automatisk — styres av Unleash-toggle i PortefoljeAktivitetKafkaConsumer.
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
        }
}
