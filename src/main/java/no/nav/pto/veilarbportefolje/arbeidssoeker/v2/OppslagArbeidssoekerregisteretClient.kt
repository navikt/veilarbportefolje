package no.nav.pto.veilarbportefolje.arbeidssoeker.v2


import jakarta.ws.rs.core.HttpHeaders
import no.nav.common.rest.client.RestUtils
import no.nav.common.utils.UrlUtils
import no.nav.pto.veilarbportefolje.util.deserializeJsonOrThrow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.http.HttpStatus
import java.time.ZonedDateTime
import java.util.*
import java.util.function.Supplier

class OppslagArbeidssoekerregisteretClient(
    private val url: String,
    private val tokenSupplier: Supplier<String>,
    private val client: OkHttpClient,
    private val consumerId: String
) {
    fun hentArbeidssokerPerioder(identitetsnummer: String): List<ArbeidssokerperiodeResponse>? {
        val request: Request = Request.Builder()
            .url(UrlUtils.joinPaths(url, "/api/v1/veileder/arbeidssoekerperioder"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenSupplier.get()}")
            .header("Nav-Consumer-Id", consumerId)
            .post(RestUtils.toJsonRequestBody(ArbeidssoekerperiodeRequest(identitetsnummer)))
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == HttpStatus.NOT_FOUND.value()) {
                return null
            }

            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }
    }

    fun hentOpplysningerOmArbeidssoeker(
        identitetsnummer: String,
        periodeId: UUID
    ): List<OpplysningerOmArbeidssoekerResponse>? {
        val request: Request = Request.Builder()
            .url(UrlUtils.joinPaths(url, "/api/v1/veileder/opplysninger-om-arbeidssoeker"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenSupplier.get()}")
            .header("Nav-Consumer-Id", consumerId)
            .post(RestUtils.toJsonRequestBody(OpplysningerOmArbeidssoekerRequest(identitetsnummer, periodeId)))
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == HttpStatus.NOT_FOUND.value()) {
                return null
            }

            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }
    }

    fun hentProfilering(identitetsnummer: String, periodeId: UUID): List<ProfileringResponse>? {
        val request: Request = Request.Builder()
            .url(UrlUtils.joinPaths(url, "/api/v1/veileder/profilering"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${tokenSupplier.get()}")
            .header("Nav-Consumer-Id", consumerId)
            .post(RestUtils.toJsonRequestBody(ProfileringRequest(identitetsnummer, periodeId)))
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == HttpStatus.NOT_FOUND.value()) {
                return null
            }

            RestUtils.throwIfNotSuccessful(response)

            return response.deserializeJsonOrThrow()
        }
    }
}

// Arbeidssøkerperioder typer
data class ArbeidssoekerperiodeRequest(val identitetsnummer: String)

data class ArbeidssokerperiodeResponse(
    val periodeId: UUID,
    val startet: MetadataResponse,
    val avsluttet: MetadataResponse?
)

// Opplysninger om arbeidssøker typer
data class OpplysningerOmArbeidssoekerRequest(val identitetsnummer: String, val periodeId: UUID)

data class OpplysningerOmArbeidssoekerResponse(
    val opplysningerOmArbeidssoekerId: UUID,
    val periodeId: UUID,
    val sendtInnAv: MetadataResponse,
    val utdanning: UtdanningResponse?,
    val helse: HelseResponse?,
    val annet: AnnetResponse?,
    val jobbsituasjon: List<BeskrivelseMedDetaljerResponse>
)

data class BeskrivelseMedDetaljerResponse(
    val beskrivelse: JobbSituasjonBeskrivelse,
    val detaljer: Map<String, String>
)

enum class JobbSituasjonBeskrivelse {
    UKJENT_VERDI,
    UDEFINERT,
    HAR_SAGT_OPP,
    HAR_BLITT_SAGT_OPP,
    ER_PERMITTERT,
    ALDRI_HATT_JOBB,
    IKKE_VAERT_I_JOBB_SISTE_2_AAR,
    AKKURAT_FULLFORT_UTDANNING,
    VIL_BYTTE_JOBB,
    USIKKER_JOBBSITUASJON,
    MIDLERTIDIG_JOBB,
    DELTIDSJOBB_VIL_MER,
    NY_JOBB,
    KONKURS,
    ANNET
}

data class AnnetResponse(
    val andreForholdHindrerArbeid: JaNeiVetIkke?
)

data class HelseResponse(
    val helsetilstandHindrerArbeid: JaNeiVetIkke
)

data class UtdanningResponse(
    val nus: String,    // TODO: Legg til kommentar som beskriver hva nus er
    val bestaatt: JaNeiVetIkke?,
    val godkjent: JaNeiVetIkke?
)

// Profilering typer
data class ProfileringRequest(val identitetsnummer: String, val periodeId: UUID)

enum class ProfilertTil {
    UKJENT_VERDI,
    UDEFINERT,
    ANTATT_GODE_MULIGHETER,
    ANTATT_BEHOV_FOR_VEILEDNING,
    OPPGITT_HINDRINGER
}

// Felles typer
enum class JaNeiVetIkke {
    JA, NEI, VET_IKKE
}

data class MetadataResponse(
    val tidspunkt: ZonedDateTime,
    val utfoertAv: BrukerResponse,
    val kilde: String,
    val aarsak: String
)

data class BrukerResponse(
    val type: BrukerType
)

enum class BrukerType {
    UKJENT_VERDI, UDEFINERT, VEILEDER, SYSTEM, SLUTTBRUKER
}
