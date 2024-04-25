package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

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


fun OpplysningerOmArbeidssoekerResponse.toOpplysningerOmArbeidssoeker() = OpplysningerOmArbeidssoeker(
    opplysningerOmArbeidssoekerId = this.opplysningerOmArbeidssoekerId,
    periodeId = this.periodeId,
    sendtInnTidspunkt = this.sendtInnAv.tidspunkt,
    utdanningNusKode = this.utdanning?.nus.orEmpty(),
    utdanningBestatt = this.utdanning?.bestaatt?.name.orEmpty(),
    utdanningGodkjent = this.utdanning?.godkjent?.name.orEmpty(),
    opplysningerOmJobbsituasjon = OpplysningerOmArbeidssoekerJobbsituasjon(this.opplysningerOmArbeidssoekerId, this.jobbsituasjon.map { it.beskrivelse.name })
)