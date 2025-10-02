package no.nav.pto.veilarbportefolje.tiltakspenger

import no.nav.common.rest.client.RestClient.baseClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRequest
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerResponseDto
import no.nav.pto.veilarbportefolje.util.deserializeJsonOrThrow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import java.util.function.Supplier


class TiltakspengerClient(private val baseUrl: String, private val machineToMachineTokenSupplier: Supplier<String>) {
    private val client: OkHttpClient = baseClient()

    fun hentTiltakspenger(personnr: String, fom: String, tom: String): List<TiltakspengerResponseDto> {
        val requestBody = TiltakspengerRequest(personnr, fom, tom)

        val request = Request.Builder()
            .url(UrlUtils.joinPaths(baseUrl, "/vedtak/detaljer"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + machineToMachineTokenSupplier.get())
            .post(RestUtils.toJsonRequestBody(requestBody))
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }

    }
}
