package no.nav.pto.veilarbportefolje.aap.controller

import no.nav.common.rest.client.RestClient.baseClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.rest.client.RestUtils.MEDIA_TYPE_JSON
import no.nav.common.utils.UrlUtils
import no.nav.pto.veilarbportefolje.aap.client.AapIPeriodeRequest
import no.nav.pto.veilarbportefolje.aap.client.AapRequest
import no.nav.pto.veilarbportefolje.aap.client.AapResponseDto
import no.nav.pto.veilarbportefolje.aap.client.AapResponseMaksimumDto
import no.nav.pto.veilarbportefolje.util.deserializeJsonOrThrow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import java.time.LocalDate
import java.util.function.Supplier

class AapClient(private val baseUrl: String, private val machineToMachineTokenSupplier: Supplier<String>) {
    private val client: OkHttpClient = baseClient()

    fun hentAapForPersonnr(personnr: String): List<AapResponseDto> {
        val requestBody = AapRequest(listOf(personnr))

        val request = Request.Builder()
            .url(UrlUtils.joinPaths(baseUrl, "/kelvin/sakerByFnr"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + machineToMachineTokenSupplier.get())
            .post(RestUtils.toJsonRequestBody(requestBody))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }
    }


    fun hentAapMaximum(personnr: String, fom: String, tom: String): List<AapResponseMaksimumDto> {
        val requestBody = AapIPeriodeRequest(personnr, fom, tom)

        val request = Request.Builder()
            .url(UrlUtils.joinPaths(baseUrl, "/kelvin/maksimumUtenUtbetaling"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + machineToMachineTokenSupplier.get())
            .post(RestUtils.toJsonRequestBody(requestBody))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }

    }
}
