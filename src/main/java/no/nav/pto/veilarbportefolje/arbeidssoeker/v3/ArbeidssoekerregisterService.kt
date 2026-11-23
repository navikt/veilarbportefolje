package no.nav.pto.veilarbportefolje.arbeidssoeker.v3

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene.*
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.DateUtils.toZonedDateTime
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
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
    private val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt,
    transactionManager: PlatformTransactionManager
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun behandleKafkaMeldingLogikk(periode: PeriodeKafkaMelding) {
        val fnr: Fnr = Fnr.ofValidFnr(periode.identitetsnummer.toString())
        if (!erUnderOppfolging(fnr)) return
        val aktorId = pdlIdentRepository.hentAktivAktorIdForPerson(fnr.toString())
        val oppfolgingStarttidspunkt = hentOppfolgingStarttidspunkt(aktorId)
        val periodeEntity = periode.toPeriodeEntity()

        val sluttDatoArbeidssoekerperiode = periodeEntity.periodeAvsluttetTidspunkt
        if (sluttDatoArbeidssoekerperiode?.isBefore(oppfolgingStarttidspunkt) == true) {
            secureLog.info(
                "Periode med id: ${periode.id} er ikke innenfor oppfølgingsperioden for bruker med fnr: ${fnr.get()}. " +
                        "Ignorerer kafkamelding for periode for bruker."
            )
            return
        }

        val alleFnrForPerson = hentAlleFnrForPerson(fnr)
        val eksisterendePeriode = hentNyesteEksisterendePeriode(alleFnrForPerson)
        val erSammePeriode = eksisterendePeriode?.periodeId == periodeEntity.periodeId

        if (eksisterendePeriode != null) {
            val kafkaErNyere = if (erSammePeriode) {
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
        }

        if (eksisterendePeriode != null && erSammePeriode) {
            secureLog.info(
                "Periode med id: ${periode.id} er oppdatert for bruker med fnr: ${fnr.get()}. " +
                        "Oppdaterer periode og beholder opplysninger og profilering."
            )
            arbeidssoekerregisterRepository.oppdaterPeriode(periodeEntity)
            opensearchIndexerPaDatafelt.oppdaterArbeidssoekerregisteret(
                aktorId,
                eksisterendePeriode.copy(
                    periodeStartTidspunkt = toZonedDateTime(periodeEntity.periodeStartTidspunkt),
                    periodeAvsluttetTidspunkt = toZonedDateTime(periodeEntity.periodeAvsluttetTidspunkt)
                ).toArbeidssoekerregisteretForOpensearch()
            )
            return
        }

        val sistePeriodeFraApi = filtrerArbeidssøkerPerioderIOppfolgingsperiode(
            fnr,
            hentAggregertPeriodeFraApi(fnr),
            oppfolgingStarttidspunkt
        )

        val lagretArbeidssoekerregisteret = transactionTemplate.execute {
            slettArbeidssoekerregisterData(fnr, alleFnrForPerson)

            if (sistePeriodeFraApi?.periodeId == periodeEntity.periodeId) {
                val periodeSomLagres = sistePeriodeFraApi.copy(
                    periodeStartTidspunkt = toZonedDateTime(periodeEntity.periodeStartTidspunkt),
                    periodeAvsluttetTidspunkt = toZonedDateTime(periodeEntity.periodeAvsluttetTidspunkt)
                )
                arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, periodeSomLagres)
                periodeSomLagres.toArbeidssoekerregisteretForOpensearch()
            } else {
                secureLog.info(
                    "Fant ingen aggregert periode med id: ${periode.id} for bruker med fnr: ${fnr.get()}. " +
                            "Api er ute av sync med kafka. Lagrer periode med synkroniseringsstatus api ute av synk."
                )
                arbeidssoekerregisterRepository.lagrePeriodeFraKafka(
                    periodeEntity,
                    SynkroniseringStatus.API_UTE_AV_SYNC
                )
                periodeEntity.toArbeidssoekerregisteretForOpensearch()
            }
        }
        lagretArbeidssoekerregisteret?.let {
            opensearchIndexerPaDatafelt.oppdaterArbeidssoekerregisteret(aktorId, it)
        }
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
        val sendtInnTidspunktEksisterende = eksisterendePeriode.opplysninger?.opplysningSendtInnTidspunkt
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
        opensearchIndexerPaDatafelt.oppdaterArbeidssoekerregisteret(
            pdlIdentRepository.hentAktivAktorIdForPerson(eksisterendePeriode.identitetsnummer),
            eksisterendePeriode.copy(opplysninger = opplysningerOmArbeidssoekerEntity)
                .toArbeidssoekerregisteretForOpensearch()
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
        val sendtInnTidspunktEksisterende = eksisterendePeriode.profilering?.profileringSendtInnTidspunkt
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
        val profileringEntity = profilering.toProfileringEntity()
        arbeidssoekerregisterRepository.lagreProfileringForPeriode(
            eksisterendePeriode.periodeId,
            profileringEntity
        )
        opensearchIndexerPaDatafelt.oppdaterArbeidssoekerregisteret(
            pdlIdentRepository.hentAktivAktorIdForPerson(eksisterendePeriode.identitetsnummer),
            eksisterendePeriode.copy(profilering = profileringEntity).toArbeidssoekerregisteretForOpensearch()
        )
    }

    @Transactional
    fun hentOgLagreArbeidssoekerdataVedOppfolgingStart(aktorId: AktorId) {
        val fnr = hentAktivFnr(aktorId)
        val sistePeriodeIOppfolgingsperiode = hentSistePeriodeIOppfolgingsperiode(aktorId, fnr)
        if (sistePeriodeIOppfolgingsperiode == null) {
            arbeidssoekerregisterRepository.lagreBrukerMedSynkroniseringStatusUsikker(fnr)
            return
        }
        lagreSistePeriode(aktorId, fnr, sistePeriodeIOppfolgingsperiode)
    }

    @Transactional
    fun hentOgLagreArbeidssoekerdataVedAdminJobb(aktorId: AktorId) {
        val fnr = hentAktivFnr(aktorId)
        val sistePeriodeFraApi = hentSistePeriodeIOppfolgingsperiode(aktorId, fnr)
        if (sistePeriodeFraApi == null) {
            secureLog.info("Fant ingen arbeidssøkerdata i api for bruker med fnr: $fnr. Gjør ingen endringer.")
            return
        }

        val alleFnrForPerson = hentAlleFnrForPerson(fnr)
        val nyesteEksisterendePeriode = hentNyesteEksisterendePeriode(alleFnrForPerson)
        if (nyesteEksisterendePeriode == null) {
            secureLog.info("Fant ingen eksisterende arbeidssøkerdata for bruker med fnr: $fnr. Lagrer periode fra api.")
            lagreSistePeriode(aktorId, fnr, sistePeriodeFraApi)
            return
        }

        val sammePeriodeId = sistePeriodeFraApi.periodeId == nyesteEksisterendePeriode.periodeId
        val ulikPeriodeOgApiErNyest = !sammePeriodeId &&
                sistePeriodeFraApi.periodeStartTidspunkt.isAfter(nyesteEksisterendePeriode.periodeStartTidspunkt)

        if (ulikPeriodeOgApiErNyest) {
            secureLog.info("Fant nyere periode med arbeidssøkerdata i api for bruker med fnr: $fnr. Sletter eksisterende og lagrer periode fra api.")
            slettArbeidssoekerregisterData(fnr, alleFnrForPerson)
            lagreSistePeriode(aktorId, fnr, sistePeriodeFraApi)
            return
        }

        val sammePeriodeOgApiErNyest =
            sammePeriodeId && apiDataErNyereEnnEksisterende(sistePeriodeFraApi, nyesteEksisterendePeriode)
        if (!sammePeriodeOgApiErNyest) {
            secureLog.info("Arbeidssøkerdata med samme periode fra api er ikke nyere enn eksisterende periode for bruker med fnr: $fnr. Gjør ingen endringer.")
            return
        }

        secureLog.info("Fant nyere data i samme arbeidssøkeperiode i api for bruker med fnr: $fnr. Sletter eksisterende og lagrer periode fra api.")
        slettArbeidssoekerregisterData(fnr, alleFnrForPerson)
        lagreSistePeriode(aktorId, fnr, sistePeriodeFraApi)
    }

    private fun hentAlleFnrForPerson(fnr: Fnr): List<Fnr> =
        pdlIdentRepository.hentFnrIdenterForBruker(fnr.get()).identer.map { Fnr.of(it) }

    private fun hentNyesteEksisterendePeriode(alleFnrForPerson: List<Fnr>): AggregertPeriodeEntity? =
        arbeidssoekerregisterRepository.hentArbeidssoekerregisterData(alleFnrForPerson)
            .mapNotNull { it.aggregertPeriode }
            .maxByOrNull { it.periodeStartTidspunkt }

    private fun slettArbeidssoekerregisterData(fnr: Fnr, alleFnrForPerson: List<Fnr>) {
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
            periodeFraApi.opplysninger?.opplysningSendtInnTidspunkt,
            nyesteEksisterende.opplysninger?.opplysningSendtInnTidspunkt
        )
        val erProfileringTidspunktNyere = erNyere(
            periodeFraApi.profilering?.profileringSendtInnTidspunkt,
            nyesteEksisterende.profilering?.profileringSendtInnTidspunkt
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
        val oppfolgingStarttidspunkt = hentOppfolgingStarttidspunkt(aktorId)
        val sistePeriodeFraApi = hentAggregertPeriodeFraApi(fnr)
        return filtrerArbeidssøkerPerioderIOppfolgingsperiode(fnr, sistePeriodeFraApi, oppfolgingStarttidspunkt)
    }

    private fun lagreSistePeriode(aktorId: AktorId, fnr: Fnr, periode: AggregertPeriodeEntity) {
        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, periode)
        opensearchIndexerPaDatafelt.oppdaterArbeidssoekerregisteret(
            aktorId,
            periode.toArbeidssoekerregisteretForOpensearch()
        )
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
        oppfolgingStarttidspunkt: Instant
    ): AggregertPeriodeEntity? {
        if (aggregertPeriode == null) {
            return null
        }
        val sluttDatoArbeissøkerperiode = aggregertPeriode.periodeAvsluttetTidspunkt?.toInstant()

        if (sluttDatoArbeissøkerperiode != null && sluttDatoArbeissøkerperiode.isBefore(oppfolgingStarttidspunkt)) {
            secureLog.info(
                "Arbeidssøkerperiode for bruker med fnr: $fnr er avsluttet før oppfølgingstarttidspunkt. " +
                        "SluttDatoArbeidssøkerperiode: $sluttDatoArbeissøkerperiode, OppfolgingStarttidspunkt: $oppfolgingStarttidspunkt." +
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

    fun hentOppfolgingStarttidspunkt(aktorId: AktorId): Instant {
        val oppfolgingsdata = oppfolgingRepositoryV2.hentOppfolgingMedStartdato(aktorId)
            .orElseThrow { IllegalStateException("Ingen oppfølgingsdata funnet") }

        if (oppfolgingsdata.oppfolging && oppfolgingsdata.startDato != null) {
            return oppfolgingsdata.startDato.toInstant()
        }

        secureLog.info("Fant ikke oppfolgingsdata for bruker med aktorId {}", aktorId)
        throw IllegalStateException("Bruker er ike under oppfølging")
    }

}
