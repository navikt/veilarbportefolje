package no.nav.pto.veilarbportefolje.aap

import no.nav.common.rest.client.RestClient.baseClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakRequest
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.util.deserializeJsonOrThrow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

class AapClient(private val baseUrl: String, private val machineToMachineTokenSupplier: Supplier<String>) {
    private val client: OkHttpClient = baseClient()

    fun hentAapVedtak(personnr: String, fom: String, tom: String): AapVedtakResponseDto {
        val requestBody = AapVedtakRequest(personnr, fom, tom)

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

