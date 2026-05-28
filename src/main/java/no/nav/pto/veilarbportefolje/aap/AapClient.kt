package no.nav.pto.veilarbportefolje.aap

import no.nav.common.rest.client.RestClient.baseClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils
import no.nav.pto.veilarbportefolje.aap.dto.AapVedtakRequest
import no.nav.pto.veilarbportefolje.aap.dto.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

class AapClient(private val baseUrl: String, private val machineToMachineTokenSupplier: Supplier<String>) {
    private val client: OkHttpClient = baseClient()

    fun hentAapVedtak(personnr: String, fom: String, tom: String): AapVedtakResponseDto? {
        val requestBody = AapVedtakRequest(personnr, fom, tom)

        val request = Request.Builder()
            .url(UrlUtils.joinPaths(baseUrl, "/kelvin/obo"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + machineToMachineTokenSupplier.get())
            .post(RestUtils.toJsonRequestBody(requestBody))
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404) {
                secureLog.info("AAP-klient fikk 404 fra Kelvin for personnr, returnerer null")
                return null
            }
            RestUtils.throwIfNotSuccessful(response)

            return RestUtils.parseJsonResponseOrThrow(response, AapVedtakResponseDto::class.java)
        }

    }
}
