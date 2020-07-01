package no.nav.pto.veilarbportefolje.feed.consumer;

import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;


public class FeedHealthCheck implements HealthCheck {

    private final FeedConsumer feedConsumer;

    public FeedHealthCheck(FeedConsumer feedConsumer) {
        this.feedConsumer = feedConsumer;
    }

    @Override
    public HealthCheckResult checkHealth() {
        try {
            if (this.feedConsumer.fetchChanges().isSuccessful()) {
                return HealthCheckResult.healthy();
            } else {
                return HealthCheckResult.unhealthy()
                        //.feilet(pingMetadata, "HTTP status " + status);
            }
        } catch (Throwable e) {
            return HealthCheckResult.unhealthy();
        }
    }
}