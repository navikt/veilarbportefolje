package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.util.DateUtils
import java.time.ZonedDateTime
import java.util.UUID
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker as OpplysningerOmArbeidssoekerKafkaMelding
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering as ProfileringKafkaMelding

/*data class OpplysningerOmArbeidssoeker(
    val opplysningerOmArbeidssoekerId: UUID,
    val periodeId: UUID,
    val sendtInnTidspunkt: ZonedDateTime,
    val utdanningNusKode: String?,
    val utdanningBestatt: String?,
    val utdanningGodkjent: String?,
    val opplysningerOmJobbsituasjon: OpplysningerOmArbeidssoekerJobbsituasjon
)*/

/*data class OpplysningerOmArbeidssoekerJobbsituasjon(
    val opplysningerOmArbeidssoekerId: UUID,
    val jobbsituasjon: List<JobbSituasjonBeskrivelse>
)*/

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

/*data class ArbeidssoekerPeriode(
    val arbeidssoekerperiodeId: UUID,
    val fnr: Fnr
)*/

/*data class Profilering(
    val periodeId: UUID,
    val profileringsresultat: Profileringsresultat,
    val sendtInnTidspunkt: ZonedDateTime
)*/

enum class Profileringsresultat {
    UKJENT_VERDI,
    UDEFINERT,
    ANTATT_GODE_MULIGHETER,
    ANTATT_BEHOV_FOR_VEILEDNING,
    OPPGITT_HINDRINGER
}







