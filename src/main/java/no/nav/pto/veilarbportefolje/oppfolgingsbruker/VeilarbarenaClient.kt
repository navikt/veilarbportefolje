package no.nav.pto.veilarbportefolje.oppfolgingsbruker

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.ws.rs.core.HttpHeaders
import no.nav.common.rest.client.RestUtils
import no.nav.common.rest.client.RestUtils.parseJsonResponseOrThrow
import no.nav.common.rest.client.RestUtils.toJsonRequestBody
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.UrlUtils.joinPaths
import no.nav.pto.veilarbportefolje.auth.AuthService
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.ZonedDateTime
import java.util.*


class VeilarbarenaClient(
    private val veilarbarenaApiConfig: VeilarbarenaApiConfig,
    private val authService: AuthService,
    private val client: OkHttpClient
) {

    fun hentOppfolgingsbruker(fnr: Fnr): Optional<OppfolgingsbrukerDTO> {
        // TODO: Endre fra OBO-token til STS-token

        val request: Request = Request.Builder()
            .url(joinPaths(veilarbarenaApiConfig.url, "/api/v2/hent-oppfolgingsbruker"))
            .header(HttpHeaders.AUTHORIZATION, authService.getOboToken(veilarbarenaApiConfig.tokenScope))
            .post(toJsonRequestBody(HentOppfolgingsbrukerRequest(fnr)))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.code == HttpStatus.NOT_FOUND.value()) {
                    return Optional.empty()
                }

                RestUtils.throwIfNotSuccessful(response)

                return Optional.ofNullable(
                    parseJsonResponseOrThrow(
                        response,
                        OppfolgingsbrukerDTO::class.java
                    )
                )
            }
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Feil ved kall mot veilarbarena/oppfolgingsbruker"
            )
        }
    }
}

data class VeilarbarenaApiConfig(
    val url: String,
    val tokenScope: String
)

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class OppfolgingsbrukerDTO(
    val fodselsnr: String? = null,
    val formidlingsgruppekode: String? = null,
    @get:JsonProperty("nav_kontor")
    val navKontor: String? = null,
    val kvalifiseringsgruppekode: String? = null,
    val rettighetsgruppekode: String? = null,
    val hovedmaalkode: String? = null,
    @get:JsonProperty("sikkerhetstiltak_type_kode")
    val sikkerhetstiltakTypeKode: String? = null,
    @get:JsonProperty("fr_kode")
    val frKode: String? = null,
    @get:JsonProperty("har_oppfolgingssak")
    val harOppfolgingssak: Boolean? = null,
    @get:JsonProperty("sperret_ansatt")
    val sperretAnsatt: Boolean? = null,
    @get:JsonProperty("er_doed")
    val erDoed: Boolean? = null,
    @get:JsonProperty("doed_fra_dato")
    val doedFraDato: ZonedDateTime? = null
)

data class HentOppfolgingsbrukerRequest(val fnr: Fnr)