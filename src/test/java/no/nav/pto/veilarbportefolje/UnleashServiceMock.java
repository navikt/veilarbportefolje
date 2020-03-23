package no.nav.pto.veilarbportefolje;

import lombok.Value;
import no.finn.unleash.strategy.Strategy;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import no.nav.sbl.featuretoggle.unleash.UnleashServiceConfig;

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
