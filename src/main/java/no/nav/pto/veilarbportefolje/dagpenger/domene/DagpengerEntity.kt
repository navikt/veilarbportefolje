package no.nav.pto.veilarbportefolje.dagpenger.domene

import java.time.LocalDate

data class DagpengerEntity (
    val fom: LocalDate,
    val tom: LocalDate?,
    val rettighetstype: DagpengerRettighetstype,
    val antallDagerResterende: Int?,
    val datoAntallDagerBleBeregnet: LocalDate?,
)
