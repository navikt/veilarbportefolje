package no.nav.pto.veilarbportefolje.dagpenger

import no.nav.common.rest.client.RestClient.baseClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerBeregningerResponseDto
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerPerioderResponseDto
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerRequest
import no.nav.pto.veilarbportefolje.util.deserializeJsonOrThrow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

class DagpengerClient(private val baseUrl: String, private val machineToMachineTokenSupplier: Supplier<String>) {
    private val client: OkHttpClient = baseClient()

    fun hentDagpengerPerioder(personnr: String, fom: String, tom: String? = null): DagpengerPerioderResponseDto {
        val requestBody = DagpengerRequest(personnr, fom, tom)

        val request = Request.Builder()
            .url(UrlUtils.joinPaths(baseUrl, "/dagpenger/datadeling/v1/perioder"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + machineToMachineTokenSupplier.get())
            .post(RestUtils.toJsonRequestBody(requestBody))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }

    }

    fun hentDagpengerBeregninger(
        personnr: String,
        fom: String,
        tom: String? = null
    ): List<DagpengerBeregningerResponseDto> {
        val requestBody = DagpengerRequest(personnr, fom, tom)

        val request = Request.Builder()
            .url(UrlUtils.joinPaths(baseUrl, "/dagpenger/datadeling/v1/beregninger"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + machineToMachineTokenSupplier.get())
            .post(RestUtils.toJsonRequestBody(requestBody))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }

    }

}

