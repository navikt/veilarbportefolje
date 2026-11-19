package no.nav.pto.veilarbportefolje.arbeidssoeker.v3

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene.*
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
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
        val fnr: Fnr = Fnr.ofValidFnr(periode.identitetsnummer.toString())
        if (!erUnderOppfolging(fnr)) return
        val aktorId = pdlIdentRepository.hentAktivAktorIdForPerson(fnr.toString())
        val oppfolgingStartdato = hentOppfolgingStartdato(aktorId)
        val periodeEntity = periode.toPeriodeEntity()

        val sluttDatoArbeidssoekerperiode = toLocalDateOrNull(periodeEntity.periodeAvsluttetTidspunkt)
        if (sluttDatoArbeidssoekerperiode?.isBefore(oppfolgingStartdato) == true) {
            secureLog.info(
                "Periode med id: ${periode.id} er ikke innenfor oppfølgingsperioden for bruker med fnr: ${fnr.get()}. " +
                        "Ignorerer kafkamelding for periode for bruker."
            )
            return
        }

        val eksisterendePeriode = hentNyesteEksisterendePeriode(fnr)

        if (eksisterendePeriode != null) {
            val kafkaErNyere = if (eksisterendePeriode.periodeId == periodeEntity.periodeId) {
                erNyere(
                    periodeEntity.periodeAvsluttetTidspunkt,
                    eksisterendePeriode.periodeAvsluttetTidspunkt?.toInstant()
                )
            } else {
                erNyere(periodeEntity.periodeStartTidspunkt, eksisterendePeriode.periodeStartTidspunkt.toInstant())
            }
            if (!kafkaErNyere) {
                secureLog.info(
                    "Periode med id: ${periode.id} er ikke nyere enn eksisterende periode med id: ${eksisterendePeriode.periodeId} " +
                            "for bruker med fnr: ${fnr.get()}. Ignorerer kafkamelding for periode for bruker."
                )
                return
            }

            secureLog.info(
                "Periode med id: ${periode.id} er nyere enn eksisterende periode for bruker med fnr: ${fnr.get()}. " +
                        "Sletter eksisterende periode."
            )
            slettArbeidssoekerregisterData(fnr)
        }

        arbeidssoekerregisterRepository.lagrePeriodeFraKafka(periodeEntity)

        val sistePeriodeIOppfolgingsperiode = hentSistePeriodeIOppfolgingsperiode(aktorId, fnr)
        if (sistePeriodeIOppfolgingsperiode == null || sistePeriodeIOppfolgingsperiode.periodeId != periodeEntity.periodeId) {
            secureLog.info(
                "Fant ingen aggregert periode med id: ${periode.id} for bruker med fnr: ${fnr.get()}. " +
                        "Api er ute av sync med kafka. Lagrer synkroniseringsstatus api ute av synk."
            )
            arbeidssoekerregisterRepository.oppdaterSynkroniseringStatus(fnr, SynkroniseringStatus.API_UTE_AV_SYNC)
            return
        }

        arbeidssoekerregisterRepository.lagreOpplysningerForPeriode(
            periodeEntity.periodeId,
            sistePeriodeIOppfolgingsperiode.toOpplysningerOmArbeidssoekerEntity()
        )
        arbeidssoekerregisterRepository.lagreProfileringForPeriode(
            periodeEntity.periodeId,
            sistePeriodeIOppfolgingsperiode.toProfileringEntity()
        )
        arbeidssoekerregisterRepository.oppdaterSynkroniseringStatus(fnr, SynkroniseringStatus.SYNKRONISERT)
    }

    @Transactional
    fun behandleKafkaMeldingLogikk(opplysninger: OpplysningerOmArbeidssoekerKafkaMelding) {
        secureLog.info("Mottok kafkamelding med opplysninger om arbeidssøker med id: ${opplysninger.id} for periodeId: ${opplysninger.periodeId}")
        val periodeId = opplysninger.periodeId
        val opplysningerOmArbeidssoekerId = opplysninger.id

        val eksisterendePeriode: AggregertPeriodeEntity? =
            arbeidssoekerregisterRepository.hentArbeidssoekerregisterDataForPeriodeid(periodeId)
        if (eksisterendePeriode == null) {
            secureLog.info(
                "Fant ingen eksisterende periode for periodeId: $periodeId. " +
                        "Ignorerer opplysninger om arbeidssøker med id: $opplysningerOmArbeidssoekerId"
            )
            return
        }

        val sendtInnTidspunktKafka = opplysninger.sendtInnAv.tidspunkt
        val sendtInnTidspunktEksisterende = eksisterendePeriode.opplysningSendtInnTidspunkt?.toInstant()
        val kafkaErNyere = erNyere(sendtInnTidspunktKafka, sendtInnTidspunktEksisterende)
        if (!kafkaErNyere) {
            secureLog.info(
                "Opplysninger om arbeidssøker med id: $opplysningerOmArbeidssoekerId er ikke nyere enn eksisterende opplysninger. " +
                        "Ignorerer opplysninger om arbeidssøker for bruker med fnr: ${eksisterendePeriode.identitetsnummer}."
            )
            return
        }
        secureLog.info(
            "Opplysninger om arbeidssøker med id: $opplysningerOmArbeidssoekerId er nyere enn eksisterende opplysninger. " +
                    "Lagrer opplysninger om arbeidssøker for bruker med fnr: ${eksisterendePeriode.identitetsnummer}."
        )
        val opplysningerOmArbeidssoekerEntity = opplysninger.toOpplysningerOmArbeidssoekerEntity()
        arbeidssoekerregisterRepository.lagreOpplysningerForPeriode(
            eksisterendePeriode.periodeId,
            opplysningerOmArbeidssoekerEntity
        )

    }

    @Transactional
    fun behandleKafkaMeldingLogikk(profilering: ProfileringKafkaMelding) {
        secureLog.info("Mottok kafkamelding med profilering med id: ${profilering.id} for periodeId: ${profilering.periodeId}")
        val periodeId = profilering.periodeId
        val profileringId = profilering.id

        val eksisterendePeriode: AggregertPeriodeEntity? =
            arbeidssoekerregisterRepository.hentArbeidssoekerregisterDataForPeriodeid(periodeId)
        if (eksisterendePeriode == null) {
            secureLog.info(
                "Fant ingen eksisterende periode for periodeId: $periodeId. " +
                        "Ignorerer profilering med id: $profileringId"
            )
            return
        }

        val sendtInnTidspunktKafka = profilering.sendtInnAv.tidspunkt
        val sendtInnTidspunktEksisterende = eksisterendePeriode.profileringSendtInnTidspunkt?.toInstant()
        val kafkaErNyere = erNyere(sendtInnTidspunktKafka, sendtInnTidspunktEksisterende)
        if (!kafkaErNyere) {
            secureLog.info(
                "Profilering med id: $profileringId er ikke nyere enn eksisterende profilering. " +
                        "Ignorerer profilering for bruker med fnr: ${eksisterendePeriode.identitetsnummer}."
            )
            return
        }
        secureLog.info(
            "Profilering med id: $profileringId er nyere enn eksisterende profilering. " +
                    "Lagrer profilering for bruker med fnr: ${eksisterendePeriode.identitetsnummer}."
        )
        arbeidssoekerregisterRepository.lagreProfileringForPeriode(
            eksisterendePeriode.periodeId,
            profilering.toProfileringEntity()
        )
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
        if (sistePeriodeFraApi == null) {
            secureLog.info("Fant ingen arbeidssøkerdata i api for bruker med fnr: $fnr. Gjør ingen endringer.")
            return
        }

        val nyesteEksisterendePeriode = hentNyesteEksisterendePeriode(fnr)
        if (nyesteEksisterendePeriode == null) {
            secureLog.info("Fant ingen eksisterende arbeidssøkerdata for bruker med fnr: $fnr. Lagrer periode fra api.")
            arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, sistePeriodeFraApi)
            return
        }

        val sammePeriodeId = sistePeriodeFraApi.periodeId == nyesteEksisterendePeriode.periodeId
        val ulikPeriodeOgApiErNyest = !sammePeriodeId &&
                sistePeriodeFraApi.periodeStartTidspunkt.isAfter(nyesteEksisterendePeriode.periodeStartTidspunkt)

        if (ulikPeriodeOgApiErNyest) {
            secureLog.info("Fant nyere periode med arbeidssøkerdata i api for bruker med fnr: $fnr. Sletter eksisterende og lagrer periode fra api.")
            slettArbeidssoekerregisterData(fnr)
            arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, sistePeriodeFraApi)
            return
        }

        val sammePeriodeOgApiErNyest =
            sammePeriodeId && apiDataErNyereEnnEksisterende(sistePeriodeFraApi, nyesteEksisterendePeriode)
        if (!sammePeriodeOgApiErNyest) {
            secureLog.info("Arbeidssøkerdata med samme periode fra api er ikke nyere enn eksisterende periode for bruker med fnr: $fnr. Gjør ingen endringer.")
            return
        }

        secureLog.info("Fant nyere data i samme arbeidssøkeperiode i api for bruker med fnr: $fnr. Sletter eksisterende og lagrer periode fra api.")
        slettArbeidssoekerregisterData(fnr)
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
        nyesteEksisterende: AggregertPeriodeEntity
    ): Boolean {
        val erAvsluttetTidspunktNyere = erNyere(
            periodeFraApi.periodeAvsluttetTidspunkt?.toInstant(),
            nyesteEksisterende.periodeAvsluttetTidspunkt?.toInstant()
        )
        val erOpplysningerTidspunktNyere = erNyere(
            periodeFraApi.opplysningSendtInnTidspunkt?.toInstant(),
            nyesteEksisterende.opplysningSendtInnTidspunkt?.toInstant()
        )
        val erProfileringTidspunktNyere = erNyere(
            periodeFraApi.profileringSendtInnTidspunkt?.toInstant(),
            nyesteEksisterende.profileringSendtInnTidspunkt?.toInstant()
        )

        return erAvsluttetTidspunktNyere || erOpplysningerTidspunktNyere || erProfileringTidspunktNyere
    }

    // Postgres lagrer med mikrosekund-presisjon, så tidspunkt må trunkeres før sammenligning
    private fun erNyere(tidspunkt: Instant?, sammenlignetMed: Instant?): Boolean = when {
        tidspunkt == null -> false
        sammenlignetMed == null -> true
        else -> tidspunkt.truncatedTo(ChronoUnit.MICROS)
            .isAfter(sammenlignetMed.truncatedTo(ChronoUnit.MICROS))
    }

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
        val sluttDatoArbeissøkerperiode = toLocalDateOrNull(aggregertPeriode.periodeAvsluttetTidspunkt?.toInstant())

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

    fun erUnderOppfolging(fnr: Fnr): Boolean {
        val erUnderOppfolging = pdlIdentRepository.erBrukerUnderOppfolging(fnr.get())

        if (!erUnderOppfolging) {
            secureLog.info("Bruker {} er ikke under oppfølging, ignorerer arbeidssøkerregister melding.", fnr.get())
            return false
        }
        return true
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
