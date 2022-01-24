package no.nav.pto.veilarbportefolje.opensearch;


import lombok.RequiredArgsConstructor;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpensearchHealthCheck implements HealthCheck {

    public static final long FORVENTET_MINIMUM_ANTALL_DOKUMENTER = 200_000;

    private final OpensearchCountService opensearchCountService;

    @Override
    public HealthCheckResult checkHealth() {
        long antallDokumenter = opensearchCountService.getCount();

        if (antallDokumenter < FORVENTET_MINIMUM_ANTALL_DOKUMENTER) {
            String feilmelding = String.format("Antall dokumenter i opensearch (%s) er mindre enn forventet antall (%s)", antallDokumenter, FORVENTET_MINIMUM_ANTALL_DOKUMENTER);
            return HealthCheckResult.unhealthy("Feil mot Opensearch", new RuntimeException(feilmelding));
        }

        return HealthCheckResult.healthy();
    }

}