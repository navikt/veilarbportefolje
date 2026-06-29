package no.nav.pto.veilarbportefolje.oppfolging.dto

import no.nav.common.types.identer.AktorId
import no.nav.pto.veilarbportefolje.domene.VeilederId
import java.time.ZonedDateTime

class VeilederTilordnetDTO(
    val aktorId: AktorId,
    val veilederId: VeilederId?,
    val tilordnetTidspunkt: ZonedDateTime?,
)
