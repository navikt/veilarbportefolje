package no.nav.pto.veilarbportefolje.dagpenger

import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerBeregningerResponseDto
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerPerioderResponseDto
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_KILDESYSTEM
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_MELDINGSTYPE
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_TYPE
import no.nav.pto.veilarbportefolje.ytelserkafka.YtelserKafkaDTO
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dagpenger")
class DagpengerController(
    private val dagpengerClient: DagpengerClient,
    private val dagpengerService: DagpengerService
) {

    @PostMapping("/perioder")
    fun hentDagpengePerioderFraApi(@RequestParam personident: String): DagpengerPerioderResponseDto {
        val fom = "2025-01-01"
        val respons = dagpengerClient.hentDagpengerPerioder(personident, fom)

        return respons
    }

    @PostMapping("/beregninger")
    fun hentDagpengeBeregningerFraApi(@RequestParam personident: String): List<DagpengerBeregningerResponseDto> {
        val fom = "2025-01-01"
        val respons = dagpengerClient.hentDagpengerBeregninger(personident, fom)

        return respons
    }

    @PostMapping("/kafkamelding")
    fun sendDagpengerKafkamelding(@RequestParam personident: String) {
        val melding = YtelserKafkaDTO(
            personId = personident,
            meldingstype = YTELSE_MELDINGSTYPE.OPPRETT,
            ytelsestype = YTELSE_TYPE.DAGPENGER,
            kildesystem = YTELSE_KILDESYSTEM.DPSAK
        )
        dagpengerService.behandleKafkaMeldingLogikk(melding)
    }

}
