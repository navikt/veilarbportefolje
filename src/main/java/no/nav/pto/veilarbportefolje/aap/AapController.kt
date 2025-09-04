package no.nav.pto.veilarbportefolje.aap

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakRequest
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Tag(
    name = "Hent aap for personidentifikasjon fra kelvin",
    description = "Aap"
)
class AapController(
    val aapClient: AapClient,
    private val aapService: AapService
) {

    @PostMapping("/hent-aap-vedtak")
    fun hentAapVedtak(@RequestBody request: AapVedtakRequest): AapVedtakResponseDto {
        return aapClient.hentAapVedtak(
            personnr = request.personidentifikator,
            fom = request.fraOgMedDato,
            tom = request.tilOgMedDato
        )
    }

    @PostMapping("/hent-aap-vedtak-for-oppfolging-periode")
    fun hentAapVedtakForOppfolgingPeriode(@RequestBody request: AapVedtakRequest): AapVedtakResponseDto {
        return aapService.hentAapVedtakForOppfolgingPeriode(
            personIdent = request.personidentifikator
        )
    }


}

