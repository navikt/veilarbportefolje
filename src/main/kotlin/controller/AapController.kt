package controller

import client.AapResponseDto
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Tag(
    name = "Hent aap for personidentifikasjon fra kelvin",
    description = "Aap"
)
class AapController(
    val aapClient: client.AapClient
) {

    @PostMapping("/hent-aap")
    fun hentAap(): List<AapResponseDto> {
        return aapClient.hentAapForPersonnr("15518316122")
    }
}

