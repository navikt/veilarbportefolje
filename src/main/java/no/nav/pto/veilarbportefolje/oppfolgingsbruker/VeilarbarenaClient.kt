package no.nav.pto.veilarbportefolje.oppfolgingsbruker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.ws.rs.core.HttpHeaders
import no.nav.common.rest.client.RestUtils
import no.nav.common.rest.client.RestUtils.parseJsonResponseOrThrow
import no.nav.common.rest.client.RestUtils.toJsonRequestBody
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.UrlUtils.joinPaths
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.springframework.http.HttpStatus
import java.time.ZonedDateTime
import java.util.*
import java.util.function.Supplier


class VeilarbarenaClient(
    private val url: String,
    private val tokenSupplier: Supplier<String>,
    private val client: OkHttpClient
) {
    companion object {
        // Objectmapperen i common-java-modules har p.t ikke mulighet til å konfigureres. Vi trenger å registrere jackson-kotlin-module for at deserialisering skal fungere med kotlin.
        val objectMapper: ObjectMapper =
            no.nav.common.json.JsonUtils.getMapper().registerModule(KotlinModule.Builder().build())

        inline fun <reified T> Response.deserializeJson(): T? {
            return RestUtils.getBodyStr(this)
                .map { objectMapper.readValue(it, T::class.java) }
                .orElse(null)
        }
        inline fun <reified T> Response.deserializeJsonOrThrow(): T {
            return this.deserializeJson() ?: throw IllegalStateException("Unable to parse JSON object from response body")
        }
    }

    fun hentOppfolgingsbruker(fnr: Fnr): Optional<OppfolgingsbrukerDTO> {
        val request: Request = Request.Builder()
            .url(joinPaths(url, "/api/v3/hent-oppfolgingsbruker"))
            .header(HttpHeaders.AUTHORIZATION, tokenSupplier.get())
            .post(toJsonRequestBody(HentOppfolgingsbrukerRequest(fnr)))
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty()
            }

            RestUtils.throwIfNotSuccessful(response)

            return Optional.ofNullable(
                response.deserializeJsonOrThrow()
            )
        }
    }
}

data class OppfolgingsbrukerDTO(
    val fodselsnr: String? = null,
    val formidlingsgruppekode: String? = null,
    val navKontor: String? = null,
    val iservFraDato: ZonedDateTime? = null,
    val kvalifiseringsgruppekode: String? = null,
    val rettighetsgruppekode: String? = null,
    val hovedmaalkode: String? = null,
    val sikkerhetstiltakTypeKode: String? = null,
    val frKode: String? = null,
    val harOppfolgingssak: Boolean? = null,
    val sperretAnsatt: Boolean? = null,
    val erDoed: Boolean? = null,
    val doedFraDato: ZonedDateTime? = null,
    val sistEndretDato: ZonedDateTime? = null
)

data class HentOppfolgingsbrukerRequest(val fnr: Fnr)