package no.nav.pto.veilarbportefolje.kafka;

import lombok.RequiredArgsConstructor;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.service.UnleashService;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class KafkaOnpremUnleash implements Supplier<Boolean> {
    private final UnleashService unleashService;

    @Override
    public Boolean get() {
        return unleashService.isEnabled(FeatureToggle.KAFKA_ONPREM_CONSUMERS_RUNNING);
    }
}
