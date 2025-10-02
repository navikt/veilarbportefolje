package no.nav.pto.veilarbportefolje.tiltakspenger

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRequest
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerResponseDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api")
@Tag(
    name = "Hent tiltakspenger for personident",
    description = "Tiltakspenger"
)
class TiltakspengerController(val tiltakspengerClient: TiltakspengerClient) {

    @PostMapping("/hent-tiltakspenger")
    fun hentTiltakspenger(@RequestBody request: TiltakspengerRequest): List<TiltakspengerResponseDto> {
        return tiltakspengerClient.hentTiltakspenger(
            personnr = request.ident,
            fom = request.fom,
            tom = request.tom
        )
    }
}
