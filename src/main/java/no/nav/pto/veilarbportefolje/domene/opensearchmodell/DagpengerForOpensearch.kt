package no.nav.pto.veilarbportefolje.domene.opensearchmodell

import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerRettighetstype
import java.time.LocalDate

data class DagpengerForOpensearch(
    val harDagpenger: Boolean = false,
    val rettighetstype: DagpengerRettighetstype? = null,
    val antallResterendeDager: Int? = null,
    val datoAntallDagerBleBeregnet: LocalDate? = null
)
