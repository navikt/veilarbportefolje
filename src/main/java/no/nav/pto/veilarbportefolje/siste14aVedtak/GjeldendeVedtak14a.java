package no.nav.pto.veilarbportefolje.siste14aVedtak;

import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal;
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe;

import java.time.ZonedDateTime;

public record GjeldendeVedtak14a(
        Innsatsgruppe innsatsgruppe,
        Hovedmal hovedmal,
        ZonedDateTime fattetDato
) {
}