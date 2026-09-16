package no.nav.pto.veilarbportefolje.domene

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class VeilederId(
    @JsonProperty("veilederId") val value: String?
) {
    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun of(veilederId: String?) = VeilederId(veilederId)

        @JvmStatic
        fun veilederIdOrNull(veilederId: String?): VeilederId? =
            veilederId?.let { VeilederId(it) }
    }

    override fun toString() = value.orEmpty()
}
