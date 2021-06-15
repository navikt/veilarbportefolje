package no.nav.pto.veilarbportefolje.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;

@RequiredArgsConstructor
public class UnleashService implements HealthCheck {
    private final UnleashClient unleashClient;

    public boolean isEnabled(String featureToggle) {
        return this.unleashClient.isEnabled(featureToggle);
    }

    @Override
    public HealthCheckResult checkHealth() {
        return unleashClient.checkHealth();
    }
}
