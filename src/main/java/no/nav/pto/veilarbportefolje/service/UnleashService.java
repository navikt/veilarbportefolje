package no.nav.pto.veilarbportefolje.service;

import no.nav.common.featuretoggle.UnleashClient;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UnleashService implements HealthCheck {

    private final UnleashClient unleashClient;

    @Autowired
    public UnleashService(UnleashClient unleashClient) {
        this.unleashClient = unleashClient;
    }


    public boolean isEnabled(String featureToggle) {
        return this.unleashClient.isEnabled(featureToggle);
    }

    @Override
    public HealthCheckResult checkHealth() {
        return unleashClient.checkHealth();
    }
}
