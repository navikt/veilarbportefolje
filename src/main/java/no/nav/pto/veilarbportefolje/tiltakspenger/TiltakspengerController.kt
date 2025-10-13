package no.nav.pto.veilarbportefolje.tiltakspenger

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRequest
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerResponseDto
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_KILDESYSTEM
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_MELDINGSTYPE
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_TYPE
import no.nav.pto.veilarbportefolje.ytelserkafka.YtelserKafkaDTO
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
class TiltakspengerController(
    val tiltakspengerClient: TiltakspengerClient,
    private val tiltakspengerService: TiltakspengerService
) {

    @PostMapping("/hent-tiltakspenger")
    fun hentTiltakspenger(@RequestBody request: TiltakspengerRequest): List<TiltakspengerResponseDto> {
        return tiltakspengerClient.hentTiltakspenger(
            personnr = request.ident,
            fom = request.fom,
            tom = request.tom
        )
    }

    @PostMapping("/simuler-kafkamelding-for-tiltakspenger")
    fun simulerKafkameldingForTiltakspenger(@RequestBody request: TiltakspengerRequest) {
        val kafkaMelding = YtelserKafkaDTO(
            personId = request.ident,
            ytelsestype = YTELSE_TYPE.TILTAKSPENGER,
            meldingstype = YTELSE_MELDINGSTYPE.OPPRETT,
            kildesystem = YTELSE_KILDESYSTEM.TPSAK
        )
        tiltakspengerService.behandleKafkaMeldingLogikk(kafkaMelding)
    }
}
