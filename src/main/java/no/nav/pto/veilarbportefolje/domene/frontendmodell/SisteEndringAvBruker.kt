package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class SisteEndringAvBruker(
    var kategori: String,
    var tidspunkt: LocalDate,
    var aktivitetId: String?,
)
