package no.nav.pto.veilarbportefolje.dagpenger

import no.nav.common.rest.client.RestClient.baseClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerVedtakRequest
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerVedtakResponseDto
import no.nav.pto.veilarbportefolje.util.deserializeJsonOrThrow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

class DagpengerClient(private val baseUrl: String, private val machineToMachineTokenSupplier: Supplier<String>) {
    private val client: OkHttpClient = baseClient()

    fun hentDagpengerVedtak(personnr: String, fom: String): List<DagpengerVedtakResponseDto> {
        val requestBody = DagpengerVedtakRequest(personnr, fom)

        val request = Request.Builder()
            .url(UrlUtils.joinPaths(baseUrl, "")) //todo: legg til riktig endpoint når det er klart
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + machineToMachineTokenSupplier.get())
            .post(RestUtils.toJsonRequestBody(requestBody))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }

    }
}

