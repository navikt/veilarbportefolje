package no.nav.pto.veilarbportefolje.domene;

import lombok.Builder;
import lombok.Value;
import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;

@Value
@Builder
public class Vedtak14aInfo {
    ArenaInnsatsgruppe arenaInnsatsgruppe;
    Innsatsgruppe innsatsgruppe;
    ArenaHovedmal arenaHovedmal;
    Hovedmal hovedmal;
}
