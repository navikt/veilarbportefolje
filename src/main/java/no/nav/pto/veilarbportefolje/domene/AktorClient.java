package no.nav.pto.veilarbportefolje.domene;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;

public interface AktorClient extends HealthCheck {
    Fnr hentFnr(AktorId aktorId);
    AktorId hentAktorId(Fnr fnr);
}
