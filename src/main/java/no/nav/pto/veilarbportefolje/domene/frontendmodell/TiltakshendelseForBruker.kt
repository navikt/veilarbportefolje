package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.tiltakshendelse.domain.Tiltakstype
import java.time.LocalDate
import java.util.*

data class TiltakshendelseForBruker(
    val id: UUID,
    val opprettet: LocalDate,
    val tekst: String,
    val lenke: String,
    val tiltakstype: Tiltakstype
)
