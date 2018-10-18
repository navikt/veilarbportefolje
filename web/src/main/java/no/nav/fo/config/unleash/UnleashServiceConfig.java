package no.nav.fo.config.unleash;

import lombok.Builder;
import lombok.Value;
import no.finn.unleash.util.UnleashScheduledExecutor;

@Builder
@Value
public class UnleashServiceConfig {

    public String applicationName;
    public String unleashApiUrl;
    public UnleashScheduledExecutor unleashScheduledExecutor;

}
