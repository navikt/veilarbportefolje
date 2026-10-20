package no.nav.pto.veilarbportefolje.sykefravaer

import no.nav.common.utils.EnvironmentUtils
import no.nav.pto.veilarbportefolje.sykefravaer.dto.OppfolgingstilfellePersonDTO
import no.nav.pto.veilarbportefolje.sykefravaer.dto.SykefravaerRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin/test")
class SykefravaerTestController(
    private val sykefravaerClient: SykefravaerClient,
) {
    @PostMapping("/sykefravaer/client")
    fun hentOppfolgingstilfelleFraClient(@RequestBody request: SykefravaerRequest): OppfolgingstilfellePersonDTO {
        sjekkErDev()
        return sykefravaerClient.hentSykefravaersoppfolingPeriode(request.fnr)
    }

    private fun sjekkErDev() {
        if (!EnvironmentUtils.isDevelopment().orElse(false)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }
    }
}
