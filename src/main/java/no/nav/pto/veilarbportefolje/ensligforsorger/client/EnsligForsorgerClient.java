package no.nav.pto.veilarbportefolje.ensligforsorger.client;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.EnsligForsorgerResponseDto;

import java.util.Optional;

public interface EnsligForsorgerClient extends HealthCheck {
     Optional<EnsligForsorgerResponseDto> hentEnsligForsorgerOvergangsstonad(Fnr fnr);
}
