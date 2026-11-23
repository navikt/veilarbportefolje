package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class Arbeidssoekerregisteret(
    val arbeidssoekerPeriodeStartdato: LocalDate,
    val arbeidssoekerPeriodeAvsluttetdato: LocalDate?,
    val utdanningOgSituasjonSistEndret: LocalDate?
)
