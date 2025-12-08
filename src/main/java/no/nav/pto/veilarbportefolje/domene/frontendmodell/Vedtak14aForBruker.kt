package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.vedtakstotte.Hovedmal
import no.nav.pto.veilarbportefolje.vedtakstotte.Innsatsgruppe
import java.time.LocalDate

data class Vedtak14aForBruker(
    val gjeldendeVedtak14a: GjeldendeVedtak14a,
    val utkast14a: Utkast14a,
) {
    data class GjeldendeVedtak14a(
        val innsatsgruppe: Innsatsgruppe?,
        val hovedmal: Hovedmal?,
        val fattetDato: LocalDate?
    )

    data class Utkast14a(
        val status : String?,
        val statusEndret: LocalDate?,
        val ansvarligVeileder: String?
    )
}
