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
    private val machineToMachinetokenSupplier: Supplier<String>,
    private val client: OkHttpClient,
    private val consumerId: String
) {
    fun hentArbeidssokerPerioder(identitetsnummer: String): List<ArbeidssokerperiodeResponse>? {
        val request: Request = Request.Builder()
            .url(UrlUtils.joinPaths(url, "/api/v1/veileder/arbeidssoekerperioder"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachinetokenSupplier.get()}")
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
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachinetokenSupplier.get()}")
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
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachinetokenSupplier.get()}")
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

    // Henter siste aktive periode
    fun hentAggregertPeriode(identitetsnummer: String): AggregertPeriodeResponse? {
        val request: Request = Request.Builder()
            .url(UrlUtils.joinPaths(url, "/api/v3/snapshot"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${machineToMachinetokenSupplier.get()}")
            .header("Nav-Consumer-Id", consumerId)
            .post(RestUtils.toJsonRequestBody(SnapshotRequest(identitetsnummer = identitetsnummer)))
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
    val beskrivelse: JobbSituasjonBeskrivelseResponse,
    val detaljer: Map<String, String>
)

enum class JobbSituasjonBeskrivelseResponse {
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
    val nus: String,    // NUS = Standard for utdanningsgruppering (https://www.ssb.no/klass/klassifikasjoner/36/)
    val bestaatt: JaNeiVetIkke?,
    val godkjent: JaNeiVetIkke?
)

// Profilering typer
data class ProfileringRequest(val identitetsnummer: String, val periodeId: UUID)

data class ProfileringResponse(
    val profileringId: UUID,
    val periodeId: UUID,
    val opplysningerOmArbeidssoekerId: UUID,
    val sendtInnAv: MetadataResponse,
    val profilertTil: ProfilertTil,
    val jobbetSammenhengendeSeksAvTolvSisteManeder: Boolean?,
    val alder: Int?
)

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

// Snapshot (v3) typer
data class SnapshotRequest(
    val type: String = "IDENTITETSNUMMER",
    val identitetsnummer: String
)

data class AggregertPeriodeResponse(
    val id: UUID,
    val identitetsnummer: String,
    val startet: PeriodeHendelseResponse,
    val avsluttet: PeriodeHendelseResponse?,
    val opplysning: OpplysningerV3Response?,
    val profilering: ProfileringV3Response?,
    val egenvurdering: EgenvurderingV3Response?,
    val bekreftelse: BekreftelseV3Response?
)

data class PeriodeHendelseResponse(
    val sendtInnAv: MetadataV3Response,
    val tidspunkt: ZonedDateTime
)

data class OpplysningerV3Response(
    val id: UUID,
    val sendtInnAv: MetadataV3Response,
    val utdanning: UtdanningV3Response?,
    val helse: HelseV3Response?,
    val jobbsituasjon: JobbsituasjonV3Response?,
    val annet: AnnetV3Response?,
    val tidspunkt: ZonedDateTime
)

data class UtdanningV3Response(
    val nus: String,
    val bestaatt: JaNeiVetIkkeV3?,
    val godkjent: JaNeiVetIkkeV3?
)

data class HelseV3Response(
    val helsetilstandHindrerArbeid: JaNeiVetIkkeV3?
)

data class AnnetV3Response(
    val andreForholdHindrerArbeid: JaNeiVetIkkeV3?
)

data class JobbsituasjonV3Response(
    val beskrivelser: List<BeskrivelseMedDetaljerResponse>
)

data class ProfileringV3Response(
    val id: UUID,
    val opplysningerOmArbeidssokerId: UUID,
    val sendtInnAv: MetadataV3Response,
    val profilertTil: ProfilertTil,
    val jobbetSammenhengendeSeksAvTolvSisteMnd: Boolean,
    val alder: Int?,
    val tidspunkt: ZonedDateTime
)

data class EgenvurderingV3Response(
    val id: UUID,
    val profileringId: UUID,
    val sendtInnAv: MetadataV3Response,
    val profilertTil: ProfilertTil,
    val egenvurdering: ProfilertTil,
    val tidspunkt: ZonedDateTime
)

data class BekreftelseV3Response(
    val id: UUID,
    val bekreftelsesloesning: Bekreftelsesloesning,
    val status: BekreftelseStatus,
    val svar: BekreftelseSvarResponse,
    val tidspunkt: ZonedDateTime
)

data class BekreftelseSvarResponse(
    val sendtInnAv: MetadataV3Response,
    val gjelderFra: ZonedDateTime,
    val gjelderTil: ZonedDateTime,
    val harJobbetIDennePerioden: Boolean,
    val vilFortsetteSomArbeidssoeker: Boolean
)

data class MetadataV3Response(
    val tidspunkt: ZonedDateTime,
    val utfoertAv: BrukerV3Response,
    val kilde: String,
    val aarsak: String,
    val tidspunktFraKilde: TidspunktFraKildeResponse?
)

data class BrukerV3Response(
    val type: BrukerType,
    val id: String,
    val sikkerhetsnivaa: String?
)

data class TidspunktFraKildeResponse(
    val tidspunkt: ZonedDateTime,
    val avviksType: AvviksType
)

enum class JaNeiVetIkkeV3 {
    UKJENT_VERDI, JA, NEI, VET_IKKE
}

enum class Bekreftelsesloesning {
    UKJENT_VERDI, ARBEIDSSOEKERREGISTERET, DAGPENGER, FRISKMELDT_TIL_ARBEIDSFORMIDLING
}

enum class BekreftelseStatus {
    GYLDIG, UVENTET_KILDE, UTENFOR_PERIODE
}

enum class AvviksType {
    UKJENT_VERDI, FORSINKELSE, RETTING, SLETTET, TIDSPUNKT_KORRIGERT
}
