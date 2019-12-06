package no.nav.fo.veilarbportefolje;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@Value
@Builder
public class FailSafeConfig {
    private int maxRetries;
    private Duration retryDelay;
    private Duration timeout;
}
