package no.nav.pto.veilarbportefolje.ungdomsprogram

import no.nav.common.rest.client.RestClient.baseClient
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils
import no.nav.pto.veilarbportefolje.ungdomsprogram.dto.UngdomsprogramResponseDto
import no.nav.pto.veilarbportefolje.util.deserializeJsonOrThrow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpHeaders
import java.util.function.Supplier

class UngdomsprogramClient(private val baseUrl: String, private val machineToMachineTokenSupplier: Supplier<String>) {
    private val client: OkHttpClient = baseClient()

    fun hentAlleMedUngdomsprogram(): UngdomsprogramResponseDto {
        val request = Request.Builder()
            .url(UrlUtils.joinPaths(baseUrl, "/ekstern/deltakelse/alle"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + machineToMachineTokenSupplier.get())
            .build()

        client.newCall(request).execute().use { response ->
            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }

    }
}
