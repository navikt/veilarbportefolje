package no.nav.pto.veilarbportefolje.ensligforsorger.dto.output

import no.nav.pto.veilarbportefolje.domene.EnsligeForsorgereOvergangsstonad
import java.time.LocalDate

@JvmRecord
data class EnsligForsorgerOvergangsstonadTiltakDto(
    @JvmField val vedtaksPeriodetypeBeskrivelse: String?,
    @JvmField val aktivitetsplikt: Boolean?,
    @JvmField val utlopsDato: LocalDate?,
    @JvmField val yngsteBarnsFodselsdato: LocalDate?
) {
    fun toEnsligeForsorgereOpensearchDto(): EnsligeForsorgereOvergangsstonad {
        return EnsligeForsorgereOvergangsstonad(
            vedtaksPeriodetypeBeskrivelse,
            aktivitetsplikt,
            utlopsDato,
            yngsteBarnsFodselsdato
        )
    }
}
