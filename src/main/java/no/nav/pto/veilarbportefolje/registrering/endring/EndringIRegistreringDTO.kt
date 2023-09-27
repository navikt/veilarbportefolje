package no.nav.pto.veilarbportefolje.registrering.endring

import java.time.LocalDate

data class EndringIRegistreringDTO(
    val aktorId: String ? = null,
    val brukersSituasjon: String ? = null,
    val brukersSituasjonSistEndret: LocalDate? = null
)
