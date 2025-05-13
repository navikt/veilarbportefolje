package no.nav.pto.veilarbportefolje.ensligforsorger.client;

import no.nav.common.health.HealthCheck;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.EnsligForsorgerResponseDto;
import no.nav.pto.veilarbportefolje.ensligforsorger.dto.input.VedtakOvergangsstønadArbeidsoppfølging;

import java.util.Optional;

public interface EnsligForsorgerClient extends HealthCheck {
    public Optional<EnsligForsorgerResponseDto> hentEnsligForsorgerOvergangsstonad(String personIdent);
}
