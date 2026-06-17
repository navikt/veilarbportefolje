package no.nav.pto.veilarbportefolje.oppfolging.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.common.types.identer.AktorId
import no.nav.pto.veilarbportefolje.domene.VeilederId
import java.time.ZonedDateTime

class VeilederTilordnetDTO {
    var aktorId: AktorId?
    var veilederId: VeilederId?
    var tilordnetTidspunkt: ZonedDateTime?

    @JsonCreator
    constructor(
        @JsonProperty("aktorId") aktorId: String,
        @JsonProperty("veilederId") veilederId: String?,
        @JsonProperty("tilordnet") tilordnetTidspunkt: ZonedDateTime?
    ) {
        this.aktorId = AktorId(aktorId)
        this.veilederId = VeilederId(veilederId)
        this.tilordnetTidspunkt = tilordnetTidspunkt
    }

    constructor(aktorId: AktorId?, veilederId: VeilederId?, tilordnetTidspunkt: ZonedDateTime?) {
        this.aktorId = aktorId
        this.veilederId = veilederId
        this.tilordnetTidspunkt = tilordnetTidspunkt
    }
}
