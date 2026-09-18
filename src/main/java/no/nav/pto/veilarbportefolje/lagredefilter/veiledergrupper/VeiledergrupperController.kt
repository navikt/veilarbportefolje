package no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.pto.veilarbportefolje.auth.AuthService
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.LagretVeiledergruppe
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.NyVeiledergruppeRequest
import no.nav.pto.veilarbportefolje.lagredefilter.veiledergrupper.domene.OppdaterVeiledergruppeRequest
import no.nav.pto.veilarbportefolje.util.ValideringsRegler
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException


@Slf4j
@RestController
@RequestMapping("/api/lagredefilter/veiledergruppe")
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
    @GetMapping("/{enhetId}")
    fun hentVeiledergrupperForEnhet(@PathVariable enhetId: String): List<LagretVeiledergruppe> {
        ValideringsRegler.sjekkEnhet(enhetId)
        authService.innloggetVeilederHarTilgangTilEnhet(enhetId)
        val veilederGrupper = veiledergrupperService.hentVeiledergrupperForEnhet(enhetId)
        return veilederGrupper
    }

    @PostMapping("/{enhetId}")
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

    @PutMapping("/{enhetId}")
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

    @DeleteMapping("/{enhetId}/filter/{filterId}")
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
