package no.nav.pto.veilarbportefolje.arbeidssoker.registrering.endring

import java.time.LocalDate

data class EndringIArbeidssokerRegistreringDTO(
    val aktorId: String ? = null,
    val brukersSituasjon: String ? = null,
    val brukersSituasjonSistEndret: LocalDate? = null
)
