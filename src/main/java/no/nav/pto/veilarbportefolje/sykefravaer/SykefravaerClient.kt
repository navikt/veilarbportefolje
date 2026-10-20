package no.nav.pto.veilarbportefolje.sykefravaer

import no.nav.common.rest.client.RestClient.baseClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils
import no.nav.pto.veilarbportefolje.util.deserializeJsonOrThrow
import no.nav.pto.veilarbportefolje.sykefravaer.dto.OppfolgingstilfellePersonDTO
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

const val NAV_PERSONIDENT_HEADER = "nav-personident"

class SykefravaerClient(private val baseUrl: String, private val machineToMachineTokenSupplier: Supplier<String>) {
    private val client: OkHttpClient = baseClient()

    fun hentSykefravaersoppfolingPeriode(personIdent: String): OppfolgingstilfellePersonDTO {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(baseUrl, "/api/system/v1/oppfolgingstilfelle/personident"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + machineToMachineTokenSupplier.get())
            .header(NAV_PERSONIDENT_HEADER, personIdent)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }
    }
}
