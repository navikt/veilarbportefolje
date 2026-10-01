package no.nav.pto.veilarbportefolje.uforetrygd

import no.nav.common.utils.EnvironmentUtils
import no.nav.pto.veilarbportefolje.uforetrygd.dto.UforetrygdRequest
import no.nav.pto.veilarbportefolje.uforetrygd.dto.UforetrygdResponseDto
import no.nav.pto.veilarbportefolje.ytelserkafka.YtelserKafkaDTO
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/test")
class UforetrygdTestController(
    private val uforetrygdService: UforetrygdService,
    private val uforetrygdClient: UforetrygdClient,
) {
    @PostMapping("/uforetrygd/kafkamelding")
    fun behandleKafkamelding(@RequestBody melding: YtelserKafkaDTO): String {
        sjekkErDev()
        uforetrygdService.behandleKafkaMeldingLogikk(melding)
        return "Melding behandlet"
    }

    @PostMapping("/uforetrygd/client")
    fun hentUforetrygdFraClient(@RequestBody request: UforetrygdRequest): UforetrygdResponseDto? {
        sjekkErDev()
        return uforetrygdClient.hentUforetrygd(request.pid)
    }

    private fun sjekkErDev() {
        if (!EnvironmentUtils.isDevelopment().orElse(false)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }
}
