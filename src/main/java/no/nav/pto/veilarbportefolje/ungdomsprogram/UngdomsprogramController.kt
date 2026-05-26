package no.nav.pto.veilarbportefolje.ungdomsprogram

import no.nav.common.auth.context.AuthContextHolder
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.UngdomsprogramResponseDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/ungdomsprogram")
class UngdomsprogramController(
    private val ungdomsprogramClient: UngdomsprogramClient,
    private val authContextHolder: AuthContextHolder,
) {

    @PostMapping("/hent-alle-brukere")
    fun hentUngdsomsprogramFraApi(): UngdomsprogramResponseDto {
        sjekkTilgangTilAdmin()
        val respons = ungdomsprogramClient.hentAlleMedUngdomsprogram()

        return respons
    }


    private fun sjekkTilgangTilAdmin() {
        val erInternBrukerFraAzure = authContextHolder.erInternBruker()
        if (erInternBrukerFraAzure) return

        throw ResponseStatusException(HttpStatus.FORBIDDEN)
    }
}
