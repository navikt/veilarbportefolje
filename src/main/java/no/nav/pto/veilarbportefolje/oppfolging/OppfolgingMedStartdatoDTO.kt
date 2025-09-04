package no.nav.pto.veilarbportefolje.oppfolging

import java.sql.Timestamp

data class OppfolgingMedStartdatoDTO(
    val oppfolging: Boolean,
    val startDato: Timestamp?
)
