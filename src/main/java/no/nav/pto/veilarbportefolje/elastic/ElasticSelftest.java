package no.nav.pto.veilarbportefolje.elastic;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import org.springframework.stereotype.Service;

@Service
public class ElasticSelftest implements HealthCheck {

    private static final long FORVENTET_MINIMUM_ANTALL_DOKUMENTER = 200_000;
    /*
    public HelsesjekkMetadata getMetadata() {
        return new (
                "elasticsearch helsesjekk",
                String.format("http://%s/%s", getElasticHostname(), getAlias()),
                String.format("Sjekker at antall dokumenter > %s", FORVENTET_MINIMUM_ANTALL_DOKUMENTER),
                false
        );
    }
     */

    @Override
    public HealthCheckResult checkHealth() {
        long antallDokumenter = ElasticUtils.getCount();
        if (antallDokumenter < FORVENTET_MINIMUM_ANTALL_DOKUMENTER) {
            String feilmelding = String.format("Antall dokumenter i elastic (%s) er mindre enn forventet antall (%s)", antallDokumenter, FORVENTET_MINIMUM_ANTALL_DOKUMENTER);
            HealthCheckResult.unhealthy("Feil mot elastic search", new RuntimeException(feilmelding));
        }
        return HealthCheckResult.healthy();
    }
}
