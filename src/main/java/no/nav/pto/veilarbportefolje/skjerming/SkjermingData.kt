package no.nav.pto.veilarbportefolje.skjerming

import no.nav.common.types.identer.Fnr
import java.sql.Timestamp

data class SkjermingData(
    val fnr: Fnr,
    val erSkjermet: Boolean,
    val skjermetFra: Timestamp?,
    val skjermetTil: Timestamp?
)

