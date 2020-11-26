package no.nav.pto.veilarbportefolje.elastic;

import lombok.RequiredArgsConstructor;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ElasticHealthCheck implements HealthCheck {

    public static final long FORVENTET_MINIMUM_ANTALL_DOKUMENTER = 200_000;

    private final ElasticCountService elasticCountService;

    @Override
    public HealthCheckResult checkHealth() {
        long antallDokumenter = elasticCountService.getCount();

        if (antallDokumenter < FORVENTET_MINIMUM_ANTALL_DOKUMENTER) {
            String feilmelding = String.format("Antall dokumenter i elastic (%s) er mindre enn forventet antall (%s)", antallDokumenter, FORVENTET_MINIMUM_ANTALL_DOKUMENTER);
            return HealthCheckResult.unhealthy("Feil mot elastic search", new RuntimeException(feilmelding));
        }

        return HealthCheckResult.healthy();
    }

}
