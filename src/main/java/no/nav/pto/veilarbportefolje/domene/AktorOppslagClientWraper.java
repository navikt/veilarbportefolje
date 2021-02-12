package no.nav.pto.veilarbportefolje.domene;

import no.nav.common.client.pdl.AktorOppslagClient;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

public class AktorOppslagClientWraper implements AktorClient {

    private final AktorOppslagClient aktorOppslagClient;

    public AktorOppslagClientWraper(AktorOppslagClient aktorOppslagClient) {
        this.aktorOppslagClient = aktorOppslagClient;
    }

    @Override
    public Fnr hentFnr(AktorId aktorId) {
        return aktorOppslagClient.hentFnr(aktorId);
    }

    @Override
    public AktorId hentAktorId(Fnr fnr) {
        return aktorOppslagClient.hentAktorId(fnr);
    }

    @Override
    public HealthCheckResult checkHealth() {
        return aktorOppslagClient.checkHealth();
    }
}
