package no.nav.pto.veilarbportefolje.aap.domene

import no.nav.pto.veilarbportefolje.aap.dto.AapVedtakResponseDto
import java.time.LocalDate

data class AapEntity (
    val sakstatus: String,
    val maksdato: LocalDate?,
    val sisteVedtak: AapVedtakResponseDto.Vedtak
)
