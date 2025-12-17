package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class EnsligForsorgerOvergangsstonad(
    val vedtaksPeriodetype: String?,
    val harAktivitetsplikt: Boolean?,
    val utlopsDato: LocalDate?,
    val yngsteBarnsFodselsdato: LocalDate?
)
