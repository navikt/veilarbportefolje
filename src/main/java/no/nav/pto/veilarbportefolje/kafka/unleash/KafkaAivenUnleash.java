package no.nav.pto.veilarbportefolje.kafka.unleash;

import io.getunleash.DefaultUnleash;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;

import java.util.function.Supplier;

public class KafkaAivenUnleash implements Supplier<Boolean> {
    private final DefaultUnleash defaultUnleash;

    public KafkaAivenUnleash(DefaultUnleash defaultUnleash) {
        this.defaultUnleash = defaultUnleash;
    }

    @Override
    public Boolean get() {
        return defaultUnleash.isEnabled(FeatureToggle.KAFKA_AIVEN_CONSUMERS_STOP);
    }
}
