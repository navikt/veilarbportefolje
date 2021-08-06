package no.nav.pto.veilarbportefolje.kafka;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.util.concurrent.TimeUnit;

import static no.nav.common.client.utils.CacheUtils.tryCacheFirst;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.NY_KAFKA_COMMON_LIB;

@Slf4j
public abstract class KafkaCommonConsumerService<T> {
    private final Cache<String, Boolean> unleashServiceCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .maximumSize(10)
            .build();

    protected boolean isNyKafkaLibraryEnabled() {
        return tryCacheFirst(unleashServiceCache, NY_KAFKA_COMMON_LIB, () -> getUnleashService().isEnabled(NY_KAFKA_COMMON_LIB));
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
