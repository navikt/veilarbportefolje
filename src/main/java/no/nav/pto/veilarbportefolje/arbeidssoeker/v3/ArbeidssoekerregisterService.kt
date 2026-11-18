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
 * - Siste arbeidssøkerperiode (SISTE_ARBEIDSSOEKER_PERIODE)
 * - Siste opplysninger om arbeidssøker, inkl. jobbsituasjon (OPPLYSNINGER_OM_ARBEIDSSOEKER, OPPLYSNINGER_OM_ARBEIDSSOEKER_JOBBSITUASJON)
 * - Siste profilering (PROFILERING)
 *
 * Dataflyt:
 * - Kafka: Tar imot meldinger fra tre topics (periode, opplysninger om arbeidssøker og profilering).
 * - Start oppfølging og admin-endepunkt: Henter og lagrer arbeidssøkerdata fra API-et (oppslag-api, ett aggregert endepunkt `/api/v3/snapshot`).
 *   Ved ny periode på Kafka hentes opplysninger og profilering fra samme endepunkt.
 * - Avslutt oppfølging: Sletter arbeidssøkerdata.
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
        val fnr: Fnr? = pdlIdentRepository.hentAktivFnrForPerson(aktorId.toString())
        if (fnr == null) {
            secureLog.info("Fant ingen fødselsnummer for bruker med aktorId: $aktorId")
            throw RuntimeException("Fant ingen fødselsnummer for bruker")
        }

        val oppfolgingStartdato = hentOppfolgingStartdato(aktorId)
        val sistePeriodeFraApi = hentAggregertPeriodeFraApi(fnr)
        val sistePeriodeIOppfolgingsperiode =
            filtrerArbeidssøkerPerioderIOppfolgingsperiode(fnr, sistePeriodeFraApi, oppfolgingStartdato)

        if (sistePeriodeIOppfolgingsperiode == null) {
            arbeidssoekerregisterRepository.lagreBrukerMedSynkroniseringStatusUsikker(fnr)
            return
        }

        arbeidssoekerregisterRepository.lagreSisteArbeidssoekerPeriode(fnr, sistePeriodeIOppfolgingsperiode)
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
