package no.nav.pto.veilarbportefolje.siste14aVedtak;

import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;

import java.time.ZonedDateTime;

public record Siste14aVedtak(
        Innsatsgruppe innsatsgruppe,
        Hovedmal hovedmal,
        ZonedDateTime fattetDato,
        boolean fraArena
) {
}
