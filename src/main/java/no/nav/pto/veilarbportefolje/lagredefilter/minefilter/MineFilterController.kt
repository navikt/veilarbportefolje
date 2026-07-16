package no.nav.pto.veilarbportefolje.lagredefilter.minefilter

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.pto.veilarbportefolje.auth.AuthUtils.getInnloggetVeilederIdent
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.LagretFilter
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.NyttFilterRequest
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.OppdaterFilterRequest
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.domene.SortOrderRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException


@Slf4j
@RestController
@RequestMapping("/api/lagredefilter/minefilter")
@RequiredArgsConstructor
@Tag(
    name = "Lagrede filter for veileder",
    description = "Funksjonalitet for å hente, lagre, slette, og oppdatere mine filter for en veileder. "
)
class MineFilterController(
    private val mineFilterService: MineFilterService
) {

    @Operation(
        summary = "Henter alle filter for en veileder",
    )
    @GetMapping
    fun hentFilterForVeileder(): List<LagretFilter> {
        val veilederIdent = getInnloggetVeilederIdent()
        val lagredeFilterForVeileder = mineFilterService.hentFilterForVeileder(veilederIdent.toString())
        return lagredeFilterForVeileder
    }

    @PostMapping
    fun lagreNyttFilterForVeileder(
        @RequestBody nyttFilterRequest: NyttFilterRequest
    ): LagretFilter {
        val veilederIdent = getInnloggetVeilederIdent()
        val lagretFilter =
            mineFilterService.lagreNyttFilterForVeileder(veilederIdent.toString(), nyttFilterRequest)
        return lagretFilter
    }

    @PutMapping()
    fun oppdaterLagretFilterForVeileder(
        @RequestBody oppdaterFilterRequest: OppdaterFilterRequest
    ): LagretFilter {
        val veilederIdent = getInnloggetVeilederIdent()
        val oppdatertFilter =
            mineFilterService.oppdaterLagretFilterForVeileder(veilederIdent.toString(), oppdaterFilterRequest)
        return oppdatertFilter
    }

    @DeleteMapping("/{filterId}")
    fun slettFilterForVeileder(
        @PathVariable filterId: Int
    ): ResponseEntity<String> {
        val veilederIdent = getInnloggetVeilederIdent()

        val antallRaderSletta: Int = mineFilterService.slettFilterForVeileder(veilederIdent.toString(), filterId)
        if (antallRaderSletta == 0) {
            throw ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Filter med id $filterId ble ikke funnet for veileder"
            )
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()

    }

    @PostMapping("/lagresortering")
    fun lagreFilterSortering(
        @RequestBody sortOrderRequest: SortOrderRequest
    ): List<LagretFilter> {
        val veilederIdent = getInnloggetVeilederIdent()

        val lagretSortering: List<LagretFilter> =
            mineFilterService.lagreSortering(veilederIdent.toString(), sortOrderRequest)
        return lagretSortering
    }
}
