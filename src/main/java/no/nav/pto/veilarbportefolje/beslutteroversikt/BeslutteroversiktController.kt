package no.nav.pto.veilarbportefolje.beslutteroversikt

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.auth.AuthService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/v1/beslutteroversikt")
@Tag(name = "Beslutteroversikt", description = "Funksjonalitet for beslutteroversikt")
class BeslutteroversiktController(
    private val beslutteroversiktService: BeslutteroversiktService,
    private val authService: AuthService
) {

    @PostMapping("/hent-etiketter")
    @Operation(
        summary = "Hent etiketter for brukere",
        description = "Henter etiketter for en liste med brukere. Brukere veileder ikke har tilgang til, eller som ikke finnes, utelates fra responsen."
    )
    fun hentEtiketter(@RequestBody request: HentEtiketterRequest): List<EtiketterForBruker> {
        authService.innloggetVeilederHarTilgangTilOppfolging()
        // TODO: Trenger vi noe mer tilgangskontroll her?
        if (request.fnr.any { !Fnr.isValid(it) }) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Ugyldig fnr")
        }

        return beslutteroversiktService.hentEtiketter(request.fnr)
    }
}

data class HentEtiketterRequest(
    val fnr: List<String>
)

data class EtiketterForBruker(
    val fnr: String,
    val etiketter: EtiketterBeslutteroversikt
)

data class EtiketterBeslutteroversikt(
    val erDoed: Boolean,
    val diskresjonskodeFortrolig: String?,
    val kandidatForUtmelding: Boolean,
)
