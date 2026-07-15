package no.nav.lagredefilter

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.lagredefilter.domene.LagretVeiledergruppe
import no.nav.lagredefilter.domene.NyVeiledergruppeRequest
import no.nav.pto.veilarbportefolje.auth.AuthService
import no.nav.pto.veilarbportefolje.util.ValideringsRegler
import org.springframework.web.bind.annotation.*


@Slf4j
@RestController
@RequestMapping("/api/lagredefilter")
@RequiredArgsConstructor
@Tag(
    name = "Lagrede veiledergrupper",
    description = "Funksjonalitet for å hente, lagre, slette, og oppdatere lagrede veiledergrupper på enheten. "
)
class VeiledergrupperController(
    private val authService: AuthService,
    private val veiledergrupperService: VeiledergrupperService
) {

    @Operation(
        summary = "Henter alle veiledergrupper på enheten",
    )
    @GetMapping("/veiledergrupper/{enhetId}")
    fun hentVeiledergrupperForEnhet(@PathVariable enhetId: String): List<LagretVeiledergruppe> {
        ValideringsRegler.sjekkEnhet(enhetId)
        authService.innloggetVeilederHarTilgangTilEnhet(enhetId)
        val veilederGrupper = veiledergrupperService.hentVeiledergrupperForEnhet(enhetId)
        return veilederGrupper
    }

    @PostMapping("/veiledergrupper/{enhetId}")
    fun lagreVeilederGrupperForEnhet(
        @PathVariable enhetId: String,
        @RequestBody nyVeildergruppeRequest: NyVeiledergruppeRequest
    ): LagretVeiledergruppe {
        ValideringsRegler.sjekkEnhet(enhetId)
        authService.innloggetVeilederHarTilgangTilEnhet(enhetId)
        val lagretVeiledergruppe =
            veiledergrupperService.lagreNyVeiledergrupperForEnhet(enhetId, nyVeildergruppeRequest)
        return lagretVeiledergruppe

    }


}
