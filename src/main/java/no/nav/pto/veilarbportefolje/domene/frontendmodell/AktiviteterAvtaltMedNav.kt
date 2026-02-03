package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class AktiviteterAvtaltMedNav(
    val nesteUtlopsdatoForAlleAktiviteter: LocalDate?,
    val nesteUtlopsdatoForFiltrerteAktiviteter: LocalDate?,
    val nyesteUtlopteAktivitet: LocalDate?,
    val aktivitetStart: LocalDate?,
    val nesteAktivitetStart: LocalDate?,
    val forrigeAktivitetStart: LocalDate?
)
