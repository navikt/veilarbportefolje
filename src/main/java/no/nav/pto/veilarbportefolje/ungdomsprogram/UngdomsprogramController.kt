package no.nav.pto.veilarbportefolje.ungdomsprogram

import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.UngdomsprogramResponseDto
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ungdomsprogram")
class UngdomsprogramController(
    private val ungdomsprogramClient: UngdomsprogramClient,
) {

    @PostMapping("/hent-alle-brukere")
    fun hentUngdsomsprogramFraApi(): UngdomsprogramResponseDto {
        val respons = ungdomsprogramClient.hentAlleMedUngdomsprogram()

        return respons
    }


}
