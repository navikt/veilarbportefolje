package no.nav.pto.veilarbportefolje.oppfolging.domene

import java.sql.Timestamp
 import java.util.UUID

data class OppfolgingData(
    val aktoerid: String,
    val veilederId: String? = null,
    val oppfolging: Boolean,
    val nyForVeileder: Boolean,
    val manuell: Boolean,
    val startDato: Timestamp,
    val tildeltTidspunkt: Timestamp? = null,
    val oppfolgingsperiodeId: UUID,
)
