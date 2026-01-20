package no.nav.pto.veilarbportefolje.dagpenger

import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerVedtakResponseDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dagpenger")
class DagpengerController(
    private val dagpengerClient: DagpengerClient
) {

    @PostMapping("/perioder")
    fun hentDagpengePerioderFraApi(@RequestParam personident: String): DagpengerVedtakResponseDto {
        val fom = "2025-01-01"
        val tom = "2027-12-31"
        val respons = dagpengerClient.hentDagpengerVedtak(personident, fom, tom )

        return respons
    }
}
