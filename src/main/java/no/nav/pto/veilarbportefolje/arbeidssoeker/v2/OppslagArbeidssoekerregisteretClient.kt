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
    fun hentAggregertArbeidssokerPeriode(identitetsnummer: String): AggregertPeriodeResponse? {
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

enum class ProfilertTil {
    UKJENT_VERDI,
    UDEFINERT,
    ANTATT_GODE_MULIGHETER,
    ANTATT_BEHOV_FOR_VEILEDNING,
    OPPGITT_HINDRINGER
}

enum class BrukerType {
    UKJENT_VERDI, UDEFINERT, VEILEDER, SYSTEM, SLUTTBRUKER
}

data class SnapshotRequest(
    val type: String = "IDENTITETSNUMMER",
    val identitetsnummer: String
)

data class AggregertPeriodeResponse(
    val id: UUID,
    val identitetsnummer: String,
    val startet: PeriodeHendelseResponse,
    val avsluttet: PeriodeHendelseResponse?,
    val opplysning: OpplysningerResponse?,
    val profilering: ProfileringResponse?,
    val egenvurdering: EgenvurderingResponse?,
    val bekreftelse: BekreftelseResponse?
)

data class PeriodeHendelseResponse(
    val sendtInnAv: MetadataResponse,
    val tidspunkt: ZonedDateTime
)

data class OpplysningerResponse(
    val id: UUID,
    val sendtInnAv: MetadataResponse,
    val utdanning: UtdanningResponse?,
    val helse: HelseResponse?,
    val jobbsituasjon: JobbsituasjonResponse?,
    val annet: AnnetResponse?,
    val tidspunkt: ZonedDateTime
)

data class UtdanningResponse(
    val nus: String,
    val bestaatt: JaNeiVetIkke?,
    val godkjent: JaNeiVetIkke?
)

data class HelseResponse(
    val helsetilstandHindrerArbeid: JaNeiVetIkke?
)

data class AnnetResponse(
    val andreForholdHindrerArbeid: JaNeiVetIkke?
)

data class JobbsituasjonResponse(
    val beskrivelser: List<BeskrivelseMedDetaljerResponse>
)

data class ProfileringResponse(
    val id: UUID,
    val opplysningerOmArbeidssokerId: UUID,
    val sendtInnAv: MetadataResponse,
    val profilertTil: ProfilertTil,
    val jobbetSammenhengendeSeksAvTolvSisteMnd: Boolean,
    val alder: Int?,
    val tidspunkt: ZonedDateTime
)

data class EgenvurderingResponse(
    val id: UUID,
    val profileringId: UUID,
    val sendtInnAv: MetadataResponse,
    val profilertTil: ProfilertTil,
    val egenvurdering: ProfilertTil,
    val tidspunkt: ZonedDateTime
)

data class BekreftelseResponse(
    val id: UUID,
    val bekreftelsesloesning: Bekreftelsesloesning,
    val status: BekreftelseStatus,
    val svar: BekreftelseSvarResponse,
    val tidspunkt: ZonedDateTime
)

data class BekreftelseSvarResponse(
    val sendtInnAv: MetadataResponse,
    val gjelderFra: ZonedDateTime,
    val gjelderTil: ZonedDateTime,
    val harJobbetIDennePerioden: Boolean,
    val vilFortsetteSomArbeidssoeker: Boolean
)

data class MetadataResponse(
    val tidspunkt: ZonedDateTime,
    val utfoertAv: BrukerResponse,
    val kilde: String,
    val aarsak: String,
    val tidspunktFraKilde: TidspunktFraKildeResponse?
)

data class BrukerResponse(
    val type: BrukerType,
    val id: String,
    val sikkerhetsnivaa: String?
)

data class TidspunktFraKildeResponse(
    val tidspunkt: ZonedDateTime,
    val avviksType: AvviksType
)

enum class JaNeiVetIkke {
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
