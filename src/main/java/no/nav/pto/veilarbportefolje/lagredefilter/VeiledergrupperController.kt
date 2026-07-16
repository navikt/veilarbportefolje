package no.nav.pto.veilarbportefolje.lagredefilter

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.pto.veilarbportefolje.auth.AuthService
import no.nav.pto.veilarbportefolje.lagredefilter.domene.LagretVeiledergruppe
import no.nav.pto.veilarbportefolje.lagredefilter.domene.NyVeiledergruppeRequest
import no.nav.pto.veilarbportefolje.lagredefilter.domene.OppdaterVeiledergruppeRequest
import no.nav.pto.veilarbportefolje.util.ValideringsRegler
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException


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
    fun lagreNyVeiledergrupperForEnhet(
        @PathVariable enhetId: String,
        @RequestBody nyVeildergruppeRequest: NyVeiledergruppeRequest
    ): LagretVeiledergruppe {
        ValideringsRegler.sjekkEnhet(enhetId)
        authService.innloggetVeilederHarTilgangTilEnhet(enhetId)
        val lagretVeiledergruppe =
            veiledergrupperService.lagreNyVeiledergruppeForEnhet(enhetId, nyVeildergruppeRequest)
        return lagretVeiledergruppe
    }

    @PutMapping("/veiledergrupper/{enhetId}")
    fun oppdaterLagretVeilederGruppeForEnhet(
        @PathVariable enhetId: String,
        @RequestBody oppdaterVeildergruppeRequest: OppdaterVeiledergruppeRequest
    ): LagretVeiledergruppe {
        ValideringsRegler.sjekkEnhet(enhetId)
        authService.innloggetVeilederHarTilgangTilEnhet(enhetId)
        val oppdatertVeiledergruppe =
            veiledergrupperService.oppdaterVeiledergruppeForEnhet(enhetId, oppdaterVeildergruppeRequest)
        return oppdatertVeiledergruppe
    }

    @DeleteMapping("/veiledergrupper/{enhetId}/filter/{filterId}")
    fun slettVeiledergruppe(
        @PathVariable enhetId: String,
        @PathVariable filterId: Int
    ): ResponseEntity<String> {
        ValideringsRegler.sjekkEnhet(enhetId)
        authService.innloggetVeilederHarTilgangTilEnhet(enhetId)

        val antallRaderSletta: Int = veiledergrupperService.slettVeiledergruppeForEnhet(enhetId, filterId)
        if (antallRaderSletta == 0) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Filter med id $filterId ble ikke funnet for enhet $enhetId"
            )
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()

    }
}
