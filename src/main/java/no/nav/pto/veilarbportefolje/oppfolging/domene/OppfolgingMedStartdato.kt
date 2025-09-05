package no.nav.pto.veilarbportefolje.oppfolging.domene

import java.sql.Timestamp

data class OppfolgingMedStartdato(
    val oppfolging: Boolean,
    val startDato: Timestamp?
)
