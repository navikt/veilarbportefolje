package no.nav.pto.veilarbportefolje.aap.controller

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.pto.veilarbportefolje.aap.client.AapIPeriodeRequest
import no.nav.pto.veilarbportefolje.aap.client.AapResponseDto
import no.nav.pto.veilarbportefolje.aap.client.AapResponseMaksimumDto
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Tag(
    name = "Hent aap for personidentifikasjon fra kelvin",
    description = "Aap"
)
class AapController(
    val aapClient: AapClient
) {

    @PostMapping("/hent-aap")
    fun hentAap(@RequestParam personnr: String): List<AapResponseDto> {
        return aapClient.hentAapForPersonnr(personnr)
    }

    @PostMapping("/hent-aap-maximum")
    fun hentAapMaximum(@RequestBody request: AapIPeriodeRequest): List<AapResponseMaksimumDto> {
        return aapClient.hentAapMaximum(
            personnr = request.personidentifikator,
            fom = request.fraOgMedDato,
            tom = request.tilOgMedDato
        )
    }

}

