package no.nav.pto.veilarbportefolje.ensligforsorger.dto.output

import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad
import java.time.LocalDate
import java.time.LocalDateTime

@JvmRecord
data class EnsligeForsorgerOvergangsstønadTiltakDto(
    @JvmField val vedtaksPeriodetypeBeskrivelse: String?,
    @JvmField val aktivitsplikt: Boolean?,
    @JvmField val utløpsDato: LocalDate?,
    @JvmField val yngsteBarnsFødselsdato: LocalDate?,
    @JvmField val radSistEndret: LocalDateTime?
) {
    fun toEnsligeForsorgereOpensearchDto(): EnsligeForsorgereOvergangsstonad {
        return EnsligeForsorgereOvergangsstonad(
            vedtaksPeriodetypeBeskrivelse,
            aktivitsplikt,
            utløpsDato,
            yngsteBarnsFødselsdato
        )
    }
}
