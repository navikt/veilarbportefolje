package no.nav.pto.veilarbportefolje.aap

import lombok.extern.slf4j.Slf4j
import no.nav.pto.veilarbportefolje.aap.dto.AapVedtakRequest
import no.nav.pto.veilarbportefolje.aap.dto.AapVedtakResponseDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
class AapController(
    private val aapClient: AapClient,
) {

    @GetMapping("api/hent-aap-vedtak")
    fun hentData(@RequestBody aapVedtakRequest: AapVedtakRequest): AapVedtakResponseDto {
        return aapClient.hentAapVedtak(
            aapVedtakRequest.personidentifikator,
            aapVedtakRequest.fraOgMedDato,
            aapVedtakRequest.tilOgMedDato
        )
    }
}
