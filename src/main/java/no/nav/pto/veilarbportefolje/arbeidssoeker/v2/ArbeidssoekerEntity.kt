package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.pto.veilarbportefolje.util.DateUtils
import java.sql.Timestamp
import java.util.*

data class OpplysningerOmArbeidssoekerEntity(
    val opplysningerOmArbeidssoekerId: UUID,
    val periodeId: UUID,
    val sendtInnTidspunkt: Timestamp,
    val utdanningNusKode: String,
    val utdanningBestatt: String,
    val utdanningGodkjent: String,
    val opplysningerOmJobbsituasjon: OpplysningerOmArbeidssoekerJobbsituasjonEntity
)

data class OpplysningerOmArbeidssoekerJobbsituasjonEntity(
    val opplysningerOmArbeidssoekerId: UUID,
    val jobbsituasjon: List<String>
)

data class ArbeidssoekerPeriodeEntity(
    val arbeidssoekerperiodeId: UUID,
    val fnr: String
)

data class ProfileringEntity(
    val periodeId: UUID,
    val profileringsresultat: String,
    val sendtInnTidspunkt: Timestamp
)

fun ProfileringResponse.toProfilering(): ProfileringEntity {
    return ProfileringEntity(
        periodeId = this.periodeId,
        profileringsresultat = this.profilertTil.name,
        sendtInnTidspunkt = DateUtils.toTimestamp(this.sendtInnAv.tidspunkt)
    )
}

fun OppslagArbeidssoekerregisteretClient.OpplysningerOmArbeidssoekerResponse.toOpplysningerOmArbeidssoeker() =
    OpplysningerOmArbeidssoekerEntity(
        opplysningerOmArbeidssoekerId = this.opplysningerOmArbeidssoekerId,
        periodeId = this.periodeId,
        sendtInnTidspunkt = DateUtils.toTimestamp(this.sendtInnAv.tidspunkt),
        utdanningNusKode = this.utdanning?.nus.orEmpty(),
        utdanningBestatt = this.utdanning?.bestaatt?.name.orEmpty(),
        utdanningGodkjent = this.utdanning?.godkjent?.name.orEmpty(),
        opplysningerOmJobbsituasjon = OpplysningerOmArbeidssoekerJobbsituasjonEntity(
            this.opplysningerOmArbeidssoekerId,
            this.jobbsituasjon.map { it.beskrivelse.name })
    )