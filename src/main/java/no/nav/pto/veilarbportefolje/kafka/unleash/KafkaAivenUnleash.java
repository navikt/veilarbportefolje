package no.nav.pto.veilarbportefolje.kafka.unleash;

import lombok.RequiredArgsConstructor;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.service.UnleashService;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class KafkaAivenUnleash implements Supplier<Boolean> {
    private final UnleashService unleashService;

    @Override
    public Boolean get() {
        return unleashService.isEnabled(FeatureToggle.KAFKA_AIVEN_CONSUMERS_STOP);
    }
}
