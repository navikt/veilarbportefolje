package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.common.types.identer.Fnr
import java.util.UUID

data class ArbeidssoekerPeriode(
    val arbeidssoekerperiodeId: UUID,
    val fnr: Fnr
)