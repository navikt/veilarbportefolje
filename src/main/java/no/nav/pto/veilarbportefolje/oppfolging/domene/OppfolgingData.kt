package no.nav.pto.veilarbportefolje.oppfolging.domene

import java.sql.Timestamp

data class OppfolgingData(
    val aktoerid: String,
    val veilederId: String? = null,
    val oppfolging: Boolean,
    val nyForVeileder: Boolean,
    val manuell: Boolean,
    val startDato: Timestamp,
    val tildeltTidspunkt: Timestamp? = null
)
