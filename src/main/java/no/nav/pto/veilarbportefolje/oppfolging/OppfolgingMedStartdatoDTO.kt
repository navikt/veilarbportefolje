package no.nav.pto.veilarbportefolje.oppfolging

import java.sql.Timestamp
import java.time.LocalDate

data class OppfolgingMedStartdatoDTO(
    val oppfolging: Boolean,
    val startDato: Timestamp?
)
