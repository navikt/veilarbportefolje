package no.nav.pto.veilarbportefolje.arbeidssoeker.v3

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene.AggregertPeriodeEntity
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene.toAggregertPeriodeEntity
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.Instant
import java.time.temporal.ChronoUnit
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as PeriodeKafkaMelding
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering as ProfileringKafkaMelding
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker as OpplysningerOmArbeidssoekerKafkaMelding

@Service
class ArbeidssoekerregisterPeriodeKafkaMeldingService(
    private val arbeidssoekerregisterService: ArbeidssoekerregisterService
) : KafkaCommonNonKeyedConsumerService<PeriodeKafkaMelding>() {
    override fun behandleKafkaMeldingLogikk(kafkaMelding: PeriodeKafkaMelding) {
        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(kafkaMelding)
    }
}

@Service
class ArbeidssoekerregisterOpplysningerOmArbeidssoekerKafkaMeldingService(
    private val arbeidssoekerregisterService: ArbeidssoekerregisterService
) : KafkaCommonNonKeyedConsumerService<OpplysningerOmArbeidssoekerKafkaMelding>() {
    override fun behandleKafkaMeldingLogikk(kafkaMelding: OpplysningerOmArbeidssoekerKafkaMelding) {
        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(kafkaMelding)
    }
}

@Service
class ArbeidssoekerregisterProfileringKafkaMeldingService(
    private val arbeidssoekerregisterService: ArbeidssoekerregisterService
) : KafkaCommonNonKeyedConsumerService<ProfileringKafkaMelding>() {
    override fun behandleKafkaMeldingLogikk(kafkaMelding: ProfileringKafkaMelding) {
        arbeidssoekerregisterService.behandleKafkaMeldingLogikk(kafkaMelding)
    }
}

/**
 * Håndterer arbeidssøkerdata fra arbeidssøkerregisteret for brukere under oppfølging.
 *
 * Arbeidssøkerdata består av:
 * - Siste arbeidssøkerperiode
 * - Siste opplysninger om arbeidssøker, inkl. jobbsituasjon og utdanning
 * - Siste profilering
 *
 * Dataflyt:
 * - Kafka: Tar imot meldinger fra tre topics (periode, opplysninger om arbeidssøker og profilering).
 * - Start oppfølging og admin-endepunkt: Henter og lagrer arbeidssøkerdata fra API-et (oppslag-api, ett aggregert endepunkt `/api/v3/snapshot`).
 *   Ved ny periode på Kafka hentes opplysninger og profilering fra samme endepunkt.
 * - Avslutt oppfølging: Sletter arbeidssøkerdata.
 *
 * Data:
 * - Data fra arbeidssøkerregisteret (Team PAW) sitt API er kun en sammenstilling av kafka-meldinger, ikke en database med masterdata.
 * Dette betyr at apiet kan være ute av sync, og at vi har rukket å få meldinger før de har konsumert sine egne. Pga dette
 * har vi en egen "synkronisering_status" der vi er usikre, som trigger henting på et senere tidspunkt.
 * - Identer - Kafka-meldinger har kun tilknytning til identen på det tidspunktet de ble sendt. Dvs, oppslag på ny aktiv
 * ident vil ikke finne treff selv om det er data på historisk ident. Håndteres i api-oppslag.
 * - Vi tar kun vare på siste arebeidssøkerperiode som er innenfor oppfølgingsperioden.
 *
 * Gamle tabeller fra veilarbregistrering (BRUKER_REGISTRERING, BRUKER_PROFILERING og ENDRING_I_REGISTRERING)
 * oppdateres ikke lenger. De leses kun fra og supplerer dataene over ved indeksering til opensearch. De slettes også ved
 * avslutt oppfølging.
 */
@Service
class ArbeidssoekerregisterService(
    private val arbeidssoekerregisterClient: ArbeidssoekerregisterClient,
    private val pdlIdentRepository: PdlIdentRepository,
    private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    private val arbeidssoekerregisterRepository: ArbeidssoekerregisterRepository,
    private val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt
) {

    @Transactional(rollbackFor = [Exception::class])
    fun behandleKafkaMeldingLogikk(periode: PeriodeKafkaMelding) {

    }

    @Transactional
    fun behandleKafkaMeldingLogikk(opplysninger: OpplysningerOmArbeidssoekerKafkaMelding) {

    }

    @Transactional
    fun behandleKafkaMeldingLogikk(profilering: ProfileringKafkaMelding) {

    }

    @Transactional
    fun hentOgLagreArbeidssoekerdataVedOppfolgingStart(aktorId: AktorId) {
        val fnr = hentAktivFnr(aktorId)
        val sistePeriodeIOppfolgingsperiode = hentSistePeriodeIOppfolgingsperiode(aktorId, fnr)
        lagreSistePeriodeEllerMarkerUsikker(fnr, sistePeriodeIOppfolgingsperiode)
    }

    @Transactional
    fun hentOgLagreArbeidssoekerdataVedAdminJobb(aktorId: AktorId) {
        val fnr = hentAktivFnr(aktorId)
        val sistePeriodeFraApi = hentSistePeriodeIOppfolgingsperiode(aktorId, fnr)
        if (sistePeriodeFraApi == null ) {
            secureLog.info("Fant ingen arbeidssøkerdata i api for bruker med fnr: $fnr. Gjør ingen endringer.")
            return
        }

        val nyesteEksisterendePeriode = hentNyesteEksisterendePeriode(fnr)
        if (!apiDataErNyereEnnEksisterende(sistePeriodeFraApi, nyesteEksisterendePeriode)) {
            secureLog.info("Arbeidssøkerdata fra api er ikke nyere enn eksisterende data for bruker med fnr: $fnr. Gjør ingen endringer.")
            return
        }

        slettArbeidssoekerregisterData(fnr)
        secureLog.info("Fant nyere arbeidssøkerdata i api for bruker med fnr: $fnr. Lagrer periode etter evt sletting av eksisterende.")
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, sistePeriodeFraApi)
    }

    private fun hentNyesteEksisterendePeriode(fnr: Fnr): AggregertPeriodeEntity? {
        val alleFnrForPerson = pdlIdentRepository.hentFnrIdenterForBruker(fnr.get()).identer.map { Fnr.of(it) }
        return arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(alleFnrForPerson)
            .mapNotNull { it.aggregertPeriode }
            .maxByOrNull { it.periodeStartTidspunkt }
    }

    private fun slettArbeidssoekerregisterData(fnr: Fnr) {
        val alleFnrForPerson = pdlIdentRepository.hentFnrIdenterForBruker(fnr.get()).identer.map { Fnr.of(it) }
        val antallSlettet = arbeidssoekerregisterRepository.slettArbeidssoekerregisterData(alleFnrForPerson)
        if (antallSlettet > 0) {
            secureLog.info("Slettet $antallSlettet rad(er) med arbeidssøkerdata for bruker med fnr: $fnr")
        }
    }

    private fun apiDataErNyereEnnEksisterende(
        periodeFraApi: AggregertPeriodeEntity,
        nyesteEksisterende: AggregertPeriodeEntity?
    ): Boolean {
        if (nyesteEksisterende == null) {
            return true
        }
        if (periodeFraApi.periodeId == nyesteEksisterende.periodeId) {
            return periodeFraApi.tidspunkter().zip(nyesteEksisterende.tidspunkter())
                .any { (fraApi, eksisterende) -> fraApi != null && (eksisterende == null || fraApi.isAfter(eksisterende)) }
        }
        return periodeFraApi.periodeStartTidspunkt.isAfter(nyesteEksisterende.periodeStartTidspunkt)
    }

    // Postgres lagrer med mikrosekund-presisjon, så tidspunkt må trunkeres før sammenligning
    private fun AggregertPeriodeEntity.tidspunkter(): List<Instant?> =
        listOf(
            periodeStartTidspunkt,
            periodeAvsluttetTidspunkt,
            opplysningSendtInnTidspunkt,
            profileringSendtInnTidspunkt
        ).map { it?.toInstant()?.truncatedTo(ChronoUnit.MICROS) }



    private fun hentAktivFnr(aktorId: AktorId): Fnr {
        val fnr: Fnr? = pdlIdentRepository.hentAktivFnrForPerson(aktorId.toString())
        if (fnr == null) {
            secureLog.info("Fant ingen fødselsnummer for bruker med aktorId: $aktorId")
            throw RuntimeException("Fant ingen fødselsnummer for bruker")
        }
        return fnr
    }

    private fun hentSistePeriodeIOppfolgingsperiode(aktorId: AktorId, fnr: Fnr): AggregertPeriodeEntity? {
        val oppfolgingStartdato = hentOppfolgingStartdato(aktorId)
        val sistePeriodeFraApi = hentAggregertPeriodeFraApi(fnr)
        return filtrerArbeidssøkerPerioderIOppfolgingsperiode(fnr, sistePeriodeFraApi, oppfolgingStartdato)
    }

    private fun lagreSistePeriodeEllerMarkerUsikker(fnr: Fnr, periode: AggregertPeriodeEntity?) {
        if (periode == null) {
            arbeidssoekerregisterRepository.lagreBrukerMedSynkroniseringStatusUsikker(fnr)
            return
        }
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, periode)
    }

    private fun hentAggregertPeriodeFraApi(
        fnr: Fnr
    ): AggregertPeriodeEntity? {
        secureLog.info("Henter aggregert arbeidssøkerperiode fra api for bruker med fnr: $fnr")
        val aggregertPeriode: AggregertPeriodeResponse? =
            arbeidssoekerregisterClient.hentAggregertArbeidssokerPeriode(fnr.get())

        if (aggregertPeriode == null) {
            secureLog.info("Fant ingen aggregert arbeidssøkerperiode for bruker med fnr: $fnr")
            return null
        }

        return aggregertPeriode.toAggregertPeriodeEntity()
    }

    private fun filtrerArbeidssøkerPerioderIOppfolgingsperiode(
        fnr: Fnr,
        aggregertPeriode: AggregertPeriodeEntity?,
        oppfolgingStartdato: LocalDate
    ): AggregertPeriodeEntity? {
        if (aggregertPeriode == null) {
            return null
        }
        val sluttDatoArbeissøkerperiode = aggregertPeriode.periodeAvsluttetTidspunkt?.toLocalDate()

        if (sluttDatoArbeissøkerperiode != null && sluttDatoArbeissøkerperiode.isBefore(oppfolgingStartdato)) {
            secureLog.info(
                "Arbeidssøkerperiode for bruker med fnr: $fnr er avsluttet før oppfølgingstartdato. " +
                        "SluttDatoArbeidssøkerperiode: $sluttDatoArbeissøkerperiode, OppfolgingStartdato: $oppfolgingStartdato." +
                        "Lagrer ikke periode for bruker."
            )
            return null
        }
        return aggregertPeriode
    }

    fun hentOppfolgingStartdato(aktorId: AktorId): LocalDate {
        val oppfolgingsdata = oppfolgingRepositoryV2.hentOppfolgingMedStartdato(aktorId)
            .orElseThrow { IllegalStateException("Ingen oppfølgingsdata funnet") }

        if (oppfolgingsdata.oppfolging && oppfolgingsdata.startDato != null) {
            return toLocalDateOrNull(oppfolgingsdata.startDato)
        }

        secureLog.info("Fant ikke oppfolgingsdata for bruker med aktorId {}", aktorId)
        throw IllegalStateException("Bruker er ike under oppfølging")
    }

}
