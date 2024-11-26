package no.nav.pto.veilarbportefolje.hendelsesfilter.domain

import java.time.LocalDateTime

data class HendelseInnhold(
    val navn: String,
    val dato: LocalDateTime,
    val lenke: String,
    val detaljer: String
)
