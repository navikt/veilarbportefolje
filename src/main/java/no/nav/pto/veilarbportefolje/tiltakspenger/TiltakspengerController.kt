package no.nav.pto.veilarbportefolje.tiltakspenger

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakRequest
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api")
@Tag(
    name = "Hent tiltakspenger for personidentifikasjon",
    description = "Tiltakspenger"
)
class TiltakspengerController() {
    @PostMapping("/hent-tiltakspenger")
    fun hentTiltakspenger(@RequestBody request: TiltakspengerRequest): AapVedtakResponseDto {
        return aapClient.hentAapVedtak(
            personnr = request.personidentifikator,
            fom = request.fraOgMedDato,
            tom = request.tilOgMedDato
        )
    }
}
