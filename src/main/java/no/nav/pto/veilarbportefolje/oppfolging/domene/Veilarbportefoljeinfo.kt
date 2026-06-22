package no.nav.pto.veilarbportefolje.oppfolging.domene

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.NavIdent
import java.time.ZonedDateTime

data class Veilarbportefoljeinfo(
    val aktorId: AktorId? = null,
    val veilederId: NavIdent? = null,
    val erUnderOppfolging: Boolean = false,
    val nyForVeileder: Boolean = false,
    val erManuell: Boolean = false,
    val startDato: ZonedDateTime? = null,
    val tilordnetTidspunkt: ZonedDateTime? = null,
)
