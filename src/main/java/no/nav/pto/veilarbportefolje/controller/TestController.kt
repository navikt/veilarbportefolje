package no.nav.pto.veilarbportefolje.controller

import no.nav.common.utils.EnvironmentUtils
import no.nav.pto.veilarbportefolje.aktiviteter.v1.TiltaksaktivitetService
import no.nav.pto.veilarbportefolje.auth.AuthService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/test")
class TestController(
    val tiltaksaktivitetService: TiltaksaktivitetService,
    val authService: AuthService
) {

    @PostMapping("/hentTiltakSomPersonDeltarPaaBulk")
    fun hentTiltakSomPersonDeltarPaaBulk(
        @RequestBody request: HentTiltakSomPersonDeltarPaaBulkRequest
    ): ResponseEntity<Any> {
        if (!(EnvironmentUtils.isProduction()?.get() ?: false)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        return ok(tiltaksaktivitetService.hentTiltakSomPersonDeltarPaaBulk(request.personIdenter.toSet()))
    }
}

data class HentTiltakSomPersonDeltarPaaBulkRequest(
    val personIdenter: List<String>
)