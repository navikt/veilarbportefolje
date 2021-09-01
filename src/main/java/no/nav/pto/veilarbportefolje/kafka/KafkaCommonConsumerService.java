package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.NY_KAFKA_COMMON_LIB;

@Slf4j
public abstract class KafkaCommonConsumerService<T> {
    protected boolean isNyKafkaLibraryEnabled() {
        return getUnleashService().isEnabled(NY_KAFKA_COMMON_LIB);
    }

    public void behandleKafkaRecord(ConsumerRecord<String, T> kafkaMelding) {
        if (!isNyKafkaLibraryEnabled()) {
            return;
        }

        log.info(
                "Behandler kafka-melding med key: {} og offset: {}, og partition: {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.partition(),
                kafkaMelding.topic()
        );
        behandleKafkaMeldingLogikk(kafkaMelding.value());
    }

    protected abstract void behandleKafkaMeldingLogikk(T kafkaMelding);

    protected abstract UnleashService getUnleashService();
}
