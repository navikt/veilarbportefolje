package no.nav.pto.veilarbportefolje.domene;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.client.pdl.AktorOppslagClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;

@RequiredArgsConstructor
public class AktorClient implements HealthCheck {

    private final AktorOppslagClient aktorOppslagClient;
    private final AktorregisterClient aktorregisterClient;
    private final UnleashService unleashService;

    public Fnr hentFnr(AktorId aktorId) {
        if (erPdlPa(unleashService)) {
            return aktorOppslagClient.hentFnr(aktorId);
        }
        return aktorregisterClient.hentFnr(aktorId);
    }

    public AktorId hentAktorId(Fnr fnr) {
        if (erPdlPa(unleashService)) {
            return aktorOppslagClient.hentAktorId(fnr);
        }
        return aktorregisterClient.hentAktorId(fnr);
    }

    @Override
    public HealthCheckResult checkHealth() {
        return aktorOppslagClient.checkHealth();
    }


    private boolean erPdlPa(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.PDL);
    }
}
