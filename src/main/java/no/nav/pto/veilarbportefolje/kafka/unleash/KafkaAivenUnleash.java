package no.nav.pto.veilarbportefolje.kafka.unleash;

import io.getunleash.DefaultUnleash;
import lombok.RequiredArgsConstructor;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class KafkaAivenUnleash implements Supplier<Boolean> {
    private final DefaultUnleash defaultUnleash;

    @Override
    public Boolean get() {
        return defaultUnleash.isEnabled(FeatureToggle.KAFKA_AIVEN_CONSUMERS_STOP);
    }
}
