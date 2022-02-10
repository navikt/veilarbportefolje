package no.nav.pto.veilarbportefolje.domene;

import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

public class AktorClient implements HealthCheck {
    private final AktorOppslagClient aktorOppslagClient;

    public AktorClient(AktorOppslagClient aktorOppslagClient) {
        this.aktorOppslagClient = aktorOppslagClient;
    }

    public Fnr hentFnr(AktorId aktorId) {
            return aktorOppslagClient.hentFnr(aktorId);
    }

    public AktorId hentAktorId(Fnr fnr) {
            return aktorOppslagClient.hentAktorId(fnr);
    }

    @Override
    public HealthCheckResult checkHealth() {
        return aktorOppslagClient.checkHealth();
    }
}
