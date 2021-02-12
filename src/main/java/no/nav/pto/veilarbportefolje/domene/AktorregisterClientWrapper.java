package no.nav.pto.veilarbportefolje.domene;

import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

public class AktorregisterClientWrapper implements AktorClient{
    private final AktorregisterClient aktorregisterClient;

    public AktorregisterClientWrapper(AktorregisterClient aktorregisterClient) {
        this.aktorregisterClient = aktorregisterClient;
    }

    @Override
    public Fnr hentFnr(AktorId aktorId) {
        return aktorregisterClient.hentFnr(aktorId);
    }

    @Override
    public AktorId hentAktorId(Fnr fnr) {
        return hentAktorId(fnr);
    }

    @Override
    public HealthCheckResult checkHealth() {
        return aktorregisterClient.checkHealth();
    }
}
