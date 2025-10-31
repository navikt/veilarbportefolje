package no.nav.pto.veilarbportefolje.oppfolgingsvedtak14a.avvik14aVedtak;

import lombok.Builder;
import lombok.Value;
import no.nav.pto.veilarbportefolje.domene.ArenaHovedmal;
import no.nav.pto.veilarbportefolje.domene.ArenaInnsatsgruppe;
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
