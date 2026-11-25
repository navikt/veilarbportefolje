package no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene

import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat
import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.mapTilUtdanning
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.AggregertPeriodeResponse
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.SynkroniseringStatus
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.ArbeidssoekerregisteretForOpensearch
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.OpplysningerOmArbeidssoekerForOpensearch
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.ProfileringForOpensearch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*


data class AggregertPeriodeEntity(
    val identitetsnummer: String,
    val periodeId: UUID,
    val periodeStartTidspunkt: ZonedDateTime,
    val periodeAvsluttetTidspunkt: ZonedDateTime?,
    val opplysninger: OpplysningerOmArbeidssoekerEntity?,
    val profilering: ProfileringEntity?,
)

fun AggregertPeriodeResponse.toAggregertPeriodeEntity() = AggregertPeriodeEntity(
    identitetsnummer = identitetsnummer,
    periodeId = id,
    periodeStartTidspunkt = startet.tidspunkt,
    periodeAvsluttetTidspunkt = avsluttet?.tidspunkt,
    opplysninger = opplysning?.let {
        OpplysningerOmArbeidssoekerEntity(
            opplysningId = it.id,
            opplysningJobbsituasjonBeskrivelse = it.jobbsituasjon?.beskrivelser?.map { b -> b.beskrivelse.name },
            opplysningUtdanningNusKode = it.utdanning?.nus?.toIntOrNull(),
            opplysningUtdanningBestatt = it.utdanning?.bestaatt?.name,
            opplysningUtdanningGodkjent = it.utdanning?.godkjent?.name,
            opplysningSendtInnTidspunkt = it.sendtInnAv.tidspunkt.toInstant()
        )
    },
    profilering = profilering?.let {
        ProfileringEntity(
            profileringId = it.id,
            profileringResultat = it.profilertTil.name,
            profileringSendtInnTidspunkt = it.sendtInnAv.tidspunkt.toInstant()
        )
    }
)

data class PeriodeEntity(
    val periodeId: UUID,
    val identitetsnummer: String,
    val periodeStartTidspunkt: Instant,
    val periodeAvsluttetTidspunkt: Instant?
)

fun no.nav.paw.arbeidssokerregisteret.api.v1.Periode.toPeriodeEntity() =
    PeriodeEntity(
        periodeId = this.id,
        identitetsnummer = this.identitetsnummer.toString(),
        periodeStartTidspunkt = this.startet.tidspunkt,
        periodeAvsluttetTidspunkt = this.avsluttet?.tidspunkt
    )

data class OpplysningerOmArbeidssoekerEntity(
    val opplysningId: UUID,
    val opplysningJobbsituasjonBeskrivelse: List<String>?,
    val opplysningUtdanningNusKode: Int?,
    val opplysningUtdanningBestatt: String?,
    val opplysningUtdanningGodkjent: String?,
    val opplysningSendtInnTidspunkt: Instant
)

fun no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker.toOpplysningerOmArbeidssoekerEntity() =
    OpplysningerOmArbeidssoekerEntity(
        opplysningId = this.id,
        opplysningSendtInnTidspunkt = this.sendtInnAv.tidspunkt,
        opplysningUtdanningNusKode = this.utdanning?.nus?.toString()?.toIntOrNull(),
        opplysningUtdanningBestatt = this.utdanning?.bestaatt?.name,
        opplysningUtdanningGodkjent = this.utdanning?.godkjent?.name,
        opplysningJobbsituasjonBeskrivelse = this.jobbsituasjon?.beskrivelser?.map { it.beskrivelse.name }
    )

data class ProfileringEntity(
    val profileringId: UUID,
    val profileringResultat: String,
    val profileringSendtInnTidspunkt: Instant
)

fun no.nav.paw.arbeidssokerregisteret.api.v1.Profilering.toProfileringEntity() =
    ProfileringEntity(
        profileringId = this.id,
        profileringResultat = this.profilertTil.name,
        profileringSendtInnTidspunkt = this.sendtInnAv.tidspunkt
    )

data class ArbeidssoekerregisterDataEntity(
    val identitetsnummer: String,
    val aggregertPeriode: AggregertPeriodeEntity?,
    val synkroniseringStatus: SynkroniseringStatus,
    val radOpprettet: ZonedDateTime,
    val radSistEndret: ZonedDateTime
)

// Mapping fra database til opensearch modell
fun AggregertPeriodeEntity.toArbeidssoekerregisteretForOpensearch() = ArbeidssoekerregisteretForOpensearch(
    periodeStartDato = periodeStartTidspunkt.toInstant().toLocalDate(),
    periodeAvsluttetDato = periodeAvsluttetTidspunkt?.toInstant()?.toLocalDate(),
    opplysninger = opplysninger?.toOpplysningerOmArbeidssoekerForOpensearch(),
    profilering = profilering?.toProfileringForOpensearch()
)

fun PeriodeEntity.toArbeidssoekerregisteretForOpensearch() = ArbeidssoekerregisteretForOpensearch(
    periodeStartDato = periodeStartTidspunkt.toLocalDate(),
    periodeAvsluttetDato = periodeAvsluttetTidspunkt?.toLocalDate()
)

private fun OpplysningerOmArbeidssoekerEntity.toOpplysningerOmArbeidssoekerForOpensearch() =
    OpplysningerOmArbeidssoekerForOpensearch(
        brukersSituasjoner = opplysningJobbsituasjonBeskrivelse,
        utdanning = mapTilUtdanning(opplysningUtdanningNusKode?.toString()),
        utdanningBestatt = opplysningUtdanningBestatt,
        utdanningGodkjent = opplysningUtdanningGodkjent,
        opplysningerSistSendtInnDato = opplysningSendtInnTidspunkt.toLocalDate()
    )

private fun ProfileringEntity.toProfileringForOpensearch() = ProfileringForOpensearch(
    profileringsResultat = Profileringsresultat.valueOf(profileringResultat)
)

private fun Instant.toLocalDate(): LocalDate = LocalDate.ofInstant(this, ZoneId.systemDefault())


