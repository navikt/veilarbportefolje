package no.nav.pto.veilarbportefolje.domene.opensearchmodell

import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerRettighetstype
import java.time.LocalDate

data class DagpengerForOpensearch(
    val dagpenger: Boolean,
    val rettighetstype: DagpengerRettighetstype,
    val antallResterendeDager: Int?,
    val datoAntallDagerBleBeregnet: LocalDate?,
)
