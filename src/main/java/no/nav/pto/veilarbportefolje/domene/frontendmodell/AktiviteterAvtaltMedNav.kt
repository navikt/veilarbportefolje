package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.sql.Timestamp
import java.time.LocalDate

data class AktiviteterAvtaltMedNav(
    val aktiviteter: MutableMap<String, Timestamp>,
    val nyesteUtlopteAktivitet: LocalDate?,
    val nesteUtlopsdatoAktivitet: LocalDate?,
    val aktivitetStart: LocalDate?,
    val nesteAktivitetStart: LocalDate?,
    val forrigeAktivitetStart: LocalDate?
)
