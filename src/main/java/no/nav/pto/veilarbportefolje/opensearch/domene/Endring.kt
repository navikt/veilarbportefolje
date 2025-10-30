package no.nav.pto.veilarbportefolje.opensearch.domene

import lombok.Data
import lombok.experimental.Accessors

@Data
@Accessors(chain = true)
data class Endring(
    var aktivtetId: String? = null,
    var tidspunkt: String? = null,
    var erSett: String? = null
)
