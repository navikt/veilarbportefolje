package no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene

import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.AggregertPeriodeResponse
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.SynkroniseringStatus
import java.time.ZonedDateTime
import java.util.*

data class AggregertPeriodeEntity(
    val identitetsnummer: String,
    val periodeId: UUID,
    val periodeStartTidspunkt: ZonedDateTime,
    val periodeAvsluttetTidspunkt: ZonedDateTime?,
    val opplysningId: UUID?,
    val opplysningJobbsituasjonBeskrivelse: List<String>?,
    val opplysningUtdanningNusKode: Int?,
    val opplysningUtdanningBestatt: String?,
    val opplysningUtdanningGodkjent: String?,
    val opplysningSendtInnTidspunkt: ZonedDateTime?,
    val profileringId: UUID?,
    val profileringResultat: String?,
    val profileringSendtInnTidspunkt: ZonedDateTime?
)

fun AggregertPeriodeResponse.toAggregertPeriodeEntity() = AggregertPeriodeEntity(
    identitetsnummer = identitetsnummer,
    periodeId = id,
    periodeStartTidspunkt = startet.tidspunkt,
    periodeAvsluttetTidspunkt = avsluttet?.tidspunkt,
    opplysningId = opplysning?.id,
    opplysningJobbsituasjonBeskrivelse = opplysning?.jobbsituasjon?.beskrivelser?.map { it.beskrivelse.name },
    opplysningUtdanningNusKode = opplysning?.utdanning?.nus?.toIntOrNull(),
    opplysningUtdanningBestatt = opplysning?.utdanning?.bestaatt?.name,
    opplysningUtdanningGodkjent = opplysning?.utdanning?.godkjent?.name,
    opplysningSendtInnTidspunkt = opplysning?.sendtInnAv?.tidspunkt,
    profileringId = profilering?.id,
    profileringResultat = profilering?.profilertTil?.name,
    profileringSendtInnTidspunkt = profilering?.sendtInnAv?.tidspunkt
)

data class ArbeidssoekerregisterDataEntity(
    val identitetsnummer: String,
    val aggregertPeriode: AggregertPeriodeEntity?,
    val synkroniseringStatus: SynkroniseringStatus,
    val radOpprettet: ZonedDateTime
)
