package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.common.types.identer.Fnr
import java.time.ZonedDateTime
import java.util.UUID

data class OpplysningerOmArbeidssoeker(
    val opplysningerOmArbeidssoekerId: UUID,
    val periodeId: UUID,
    val sendtInnTidspunkt: ZonedDateTime,
    val utdanningNusKode: String,
    val utdanningBestatt: String,
    val utdanningGodkjent: String,
    val opplysningerOmJobbsituasjon: OpplysningerOmArbeidssoekerJobbsituasjon
)

data class OpplysningerOmArbeidssoekerJobbsituasjon(
    val opplysningerOmArbeidssoekerId: UUID,
    val jobbsituasjon: List<String>
)

data class ArbeidssoekerPeriode(
    val arbeidssoekerperiodeId: UUID,
    val fnr: Fnr
)

data class Profilering(
    val periodeId: UUID,
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

fun ProfileringResponse.toProfilering(): Profilering {
    return Profilering(
        periodeId = this.periodeId,
        profileringsresultat = Profileringsresultat.valueOf(this.profilertTil.name),
        sendtInnTidspunkt = this.sendtInnAv.tidspunkt
    )
}

fun OpplysningerOmArbeidssoekerResponse.toOpplysningerOmArbeidssoeker() = OpplysningerOmArbeidssoeker(
    opplysningerOmArbeidssoekerId = this.opplysningerOmArbeidssoekerId,
    periodeId = this.periodeId,
    sendtInnTidspunkt = this.sendtInnAv.tidspunkt,
    utdanningNusKode = this.utdanning?.nus.orEmpty(),
    utdanningBestatt = this.utdanning?.bestaatt?.name.orEmpty(),
    utdanningGodkjent = this.utdanning?.godkjent?.name.orEmpty(),
    opplysningerOmJobbsituasjon = OpplysningerOmArbeidssoekerJobbsituasjon(this.opplysningerOmArbeidssoekerId, this.jobbsituasjon.map { it.beskrivelse.name })
)