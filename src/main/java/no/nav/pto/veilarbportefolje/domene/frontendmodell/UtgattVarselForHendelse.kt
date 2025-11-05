package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.net.URL
import java.time.LocalDate

data class UtgattVarselForHendelse(
    val beskrivelse: String?,
    val dato: LocalDate?,
    val lenke: URL?,
)
