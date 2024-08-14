package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.pto.veilarbportefolje.util.DateUtils
import java.sql.Timestamp
import java.util.*

data class OpplysningerOmArbeidssoekerEntity(
    val opplysningerOmArbeidssoekerId: UUID,
    val periodeId: UUID,
    val sendtInnTidspunkt: Timestamp,
    val utdanningNusKode: String?,
    val utdanningBestatt: String?,
    val utdanningGodkjent: String?,
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


fun OpplysningerOmArbeidssoekerResponse.toOpplysningerOmArbeidssoekerEntity() = OpplysningerOmArbeidssoekerEntity(
    opplysningerOmArbeidssoekerId = this.opplysningerOmArbeidssoekerId,
    periodeId = this.periodeId,
    sendtInnTidspunkt = DateUtils.toTimestamp(this.sendtInnAv.tidspunkt),
    utdanningNusKode = this.utdanning?.nus,
    utdanningBestatt = this.utdanning?.bestaatt?.name,
    utdanningGodkjent = this.utdanning?.godkjent?.name,
    opplysningerOmJobbsituasjon = OpplysningerOmArbeidssoekerJobbsituasjonEntity(
        this.opplysningerOmArbeidssoekerId,
        this.jobbsituasjon.map { it.beskrivelse.name })
)

fun no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker.toOpplysningerOmArbeidssoekerEntity() = OpplysningerOmArbeidssoekerEntity(
    opplysningerOmArbeidssoekerId = this.id,
    periodeId = this.periodeId,
    sendtInnTidspunkt = Timestamp.from(this.sendtInnAv.tidspunkt),
    utdanningNusKode = this.utdanning?.nus.toString(),
    utdanningBestatt = this.utdanning?.bestaatt?.name,
    utdanningGodkjent = this.utdanning?.godkjent?.name,
    opplysningerOmJobbsituasjon = OpplysningerOmArbeidssoekerJobbsituasjonEntity(
        this.id,
        this.jobbsituasjon.beskrivelser.map { it.beskrivelse.name })
)

fun ProfileringResponse.toProfileringEntity(): ProfileringEntity {
    return ProfileringEntity(
        periodeId = this.periodeId,
        profileringsresultat = this.profilertTil.name,
        sendtInnTidspunkt = DateUtils.toTimestamp(this.sendtInnAv.tidspunkt)
    )
}

fun no.nav.paw.arbeidssokerregisteret.api.v1.Profilering.toProfileringEntity() = ProfileringEntity(
    periodeId = this.periodeId,
    profileringsresultat = this.profilertTil.name,
    sendtInnTidspunkt = Timestamp.from(this.sendtInnAv.tidspunkt)
)