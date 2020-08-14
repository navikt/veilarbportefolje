package no.nav.pto.veilarbportefolje.mock;

import no.finn.unleash.strategy.Strategy;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.featuretoggle.UnleashServiceConfig;

import java.util.Map;

public class UnleashServiceMock extends UnleashService {

    boolean enabled;

    public UnleashServiceMock(boolean isEnabled) {
        super(UnleashServiceConfig.builder()
                        .unleashApiUrl("http://test")
                        .applicationName("test")
                        .build(),
        new Strategy() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public boolean isEnabled(Map<String, String> parameters) {
                return false;
            }
        });

        this.enabled = isEnabled;
    }

    public boolean isEnabled(String anyString) {
        return enabled;
    }
}
