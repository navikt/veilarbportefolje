package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.OppslagArbeidssoekerregisteretClient.MetadataResponse
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.OppslagArbeidssoekerregisteretClient.ProfilertTil
import java.time.ZonedDateTime
import java.util.*

data class OpplysningerOmArbeidssoeker(
    val sendtInnTidspunkt: ZonedDateTime,
    val utdanning: Utdanning,
    val utdanningBestatt: JaNeiVetIkke,
    val utdanningGodkjent: JaNeiVetIkke,
    val jobbsituasjoner: List<JobbSituasjonBeskrivelse>
)

enum class JaNeiVetIkke {
    JA, NEI, VET_IKKE
}

enum class Utdanning {
    INGEN_UTDANNING,
    GRUNNSKOLE,
    VIDEREGAENDE_GRUNNUTDANNING,
    VIDEREGAENDE_FAGBREV_SVENNEBREV,
    HOYERE_UTDANNING_1_TIL_4,
    HOYERE_UTDANNING_5_ELLER_MER,
    INGEN_SVAR
}

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

data class ArbeidssoekerData(
    val fnr: Fnr,
    val opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker? = null,
    val profilering: Profilering? = null
)

data class Profilering(
    val profileringsresultat: Profileringsresultat,
    val sendtInnTidspunkt: ZonedDateTime
)

enum class Profileringsresultat {
    UKJENT_VERDI,
    UDEFINERT,
    ANTATT_GODE_MULIGHETER,
    ANTATT_BEHOV_FOR_VEILEDNING,
    OPPGITT_HINDRINGER
}

data class ProfileringResponse(
    val profileringId: UUID,
    val periodeId: UUID,
    val opplysningerOmArbeidssoekerId: UUID,
    val sendtInnAv: MetadataResponse,
    val profilertTil: ProfilertTil,
    val jobbetSammenhengendeSeksAvTolvSisteManeder: Boolean?,
    val alder: Int?
)
