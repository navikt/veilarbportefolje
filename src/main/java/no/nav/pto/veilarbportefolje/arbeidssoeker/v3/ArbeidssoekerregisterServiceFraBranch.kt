package no.nav.pto.veilarbportefolje.arbeidssoeker.v3

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering as ProfileringKafkaMelding
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker as OpplysningerOmArbeidssoekerKafkaMelding


//@Service
//class ArbeidssoekerregisterPeriodeKafkaMeldingService(
//    private val arbeidssoekerService: ArbeidssoekerregisterService
//) : KafkaCommonNonKeyedConsumerService<Periode>() {
//    override fun behandleKafkaMeldingLogikk(kafkaMelding: Periode) {
//        arbeidssoekerService.behandleKafkaMeldingLogikk(kafkaMelding)
//    }
//}
//
//@Service
//class ArbeidssoekerregisterOpplysningerOmArbeidssoekerKafkaMeldingService(
//    private val arbeidssoekerService: ArbeidssoekerregisterService
//) : KafkaCommonNonKeyedConsumerService<OpplysningerOmArbeidssoekerKafkaMelding>() {
//    override fun behandleKafkaMeldingLogikk(kafkaMelding: OpplysningerOmArbeidssoekerKafkaMelding) {
//        arbeidssoekerService.behandleKafkaMeldingLogikk(kafkaMelding)
//    }
//}
//
//@Service
//class ArbeidssoekerregisterProfileringKafkaMeldingService(
//    private val arbeidssoekerService: ArbeidssoekerregisterService
//) : KafkaCommonNonKeyedConsumerService<ProfileringKafkaMelding>() {
//    override fun behandleKafkaMeldingLogikk(kafkaMelding: ProfileringKafkaMelding) {
//        arbeidssoekerService.behandleKafkaMeldingLogikk(kafkaMelding)
//    }
//}

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
class ArbeidssoekerregisterServiceFraBranch(
    private val arbeidssoekerregisterClient: ArbeidssoekerregisterClient,
    private val pdlIdentRepository: PdlIdentRepository,
    private val arbeidssoekerregisterRepository: ArbeidssoekerregisterRepository,
    private val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt
) {

    @Transactional(rollbackFor = [Exception::class])
    fun behandleKafkaMeldingLogikk(kafkaMelding: Periode) {
        val periodeId = kafkaMelding.id
        val identitetsnummer = kafkaMelding.identitetsnummer

        val fnrIMelding: Fnr = Fnr.ofValidFnr(identitetsnummer.toString())
        secureLog.info("Behandler endring på arbeidssøkerperiode for bruker med fnr: $fnrIMelding, periodeId: $periodeId")

        if (!pdlIdentRepository.erBrukerUnderOppfolging(fnrIMelding.get())) {
            secureLog.info(
                "Bruker er ikke under oppfølging, ignorerer endring for arbeidssøkerregisterdata på bruker med fnr: {}",
                fnrIMelding.get()
            )
            return
        }

        val fnr: Fnr? = pdlIdentRepository.hentAktivFnrForPerson(fnrIMelding.get())
        if (fnr == null) {
            secureLog.warn("Fant ikke aktivt fnr for bruker med fnr: $fnrIMelding, ignorerer arbeidssøkerperiode $periodeId")
            return
        }

        val aktorId = pdlIdentRepository.hentAktivAktorIdForPerson(fnr.toString())

        val bleLagret = hentOgLagreAggregertPeriode(fnr, aktorId, forventetPeriodeId = periodeId)

        if (!bleLagret) {
            erstattSisteArbeidssoekerPeriode(fnr, periodeId)
        }
    }

    @Transactional
    fun behandleKafkaMeldingLogikk(opplysninger: OpplysningerOmArbeidssoekerKafkaMelding) {
//        val arbeidssoekerPeriodeId = opplysninger.periodeId
//        val opplysningerOmArbeidssoekerId = opplysninger.id
//
//        secureLog.info("Behandler endring på opplysninger om arbeidssøker for bruker med arbeidssoekerPeriodeId: $arbeidssoekerPeriodeId, opplysningerOmArbeidssoekerId: $opplysningerOmArbeidssoekerId")
//
//        val sisteArbeidssoekerPeriode =
//            sisteArbeidssoekerPeriodeRepository.hentSisteArbeidssoekerPeriode(arbeidssoekerPeriodeId)
//
//        if (sisteArbeidssoekerPeriode == null) {
//            secureLog.info("Ingen arbeidssøkerperiode lagret med arbeidssoekerPeriodeId: $arbeidssoekerPeriodeId")
//            return
//        }
//
//        val fnr = sisteArbeidssoekerPeriode.fnr
//        if (!pdlIdentRepository.erBrukerUnderOppfolging(fnr)) {
//            secureLog.info(
//                "Bruker med fnr $fnr er ikke under oppfølging, men har arbeidssøkerpeiode lagret. " +
//                        "Dette betyr at arbeidssøkerdata ikke har blitt slettet riktig når bruker gikk ut av oppfølging. " +
//                        "Ignorer melding, data må slettes manuelt og slettelogikk ved utgang av oppfølging bør kontrollsjekkes for feil."
//            )
//            return
//        }
//
//        val aktorId = pdlIdentRepository.hentAktivAktorIdForPerson(fnr)
//
//        val opplysningerOmArbeidssoeker =
//            opplysningerOmArbeidssoekerRepository.harSisteOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoekerId)
//        if (opplysningerOmArbeidssoeker) {
//            secureLog.info("Opplysninger om arbeidssøker allerede lagret for bruker med fnr: $fnr")
//            return
//        }
//
//        val opplysningerOmArbeidssoekerEntity = opplysninger.toOpplysningerOmArbeidssoekerEntity()
//        opplysningerOmArbeidssoekerRepository.slettOpplysningerOmArbeidssoeker(sisteArbeidssoekerPeriode.arbeidssoekerperiodeId)
//        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(
//            opplysningerOmArbeidssoekerEntity
//        )
//
//        if (aktorId != null) {
//            opensearchIndexerPaDatafelt.updateOpplysningerOmArbeidssoeker(aktorId, opplysningerOmArbeidssoekerEntity)
//        }
//
//        secureLog.info("Lagret opplysninger om arbeidssøker for bruker med fnr: $fnr")
    }

    @Transactional
    fun behandleKafkaMeldingLogikk(kafkaMelding: ProfileringKafkaMelding) {
/*        secureLog.info("Behandler endring på profilering for bruker med arbeidssoekerPeriodeId: ${kafkaMelding.periodeId}")

        val sisteArbeidssoekerPeriode =
            sisteArbeidssoekerPeriodeRepository.hentSisteArbeidssoekerPeriode(kafkaMelding.periodeId)

        if (sisteArbeidssoekerPeriode == null) {
            secureLog.info("Ingen arbeidssøkerperiode lagret med arbeidssoekerPeriodeId: ${kafkaMelding.periodeId}")
            return
        }

        val fnr = sisteArbeidssoekerPeriode.fnr
        if (!pdlIdentRepository.erBrukerUnderOppfolging(fnr)) {
            secureLog.info(
                "Bruker med fnr $fnr er ikke under oppfølging, men har arbeidssøkerpeiode lagret. " +
                        "Dette betyr at arbeidssøkerdata ikke har blitt slettet riktig når bruker gikk ut av oppfølging. " +
                        "Ignorer melding, data må slettes manuelt og slettelogikk ved utgang av oppfølging bør kontrollsjekkes for feil."
            )
            return
        }
        val aktorId = pdlIdentRepository.hentAktivAktorIdForPerson(fnr)
        val profileringEntity = kafkaMelding.toProfileringEntity()

        profileringRepository.insertProfilering(profileringEntity)
        if (aktorId != null) {
            opensearchIndexerPaDatafelt.updateProfilering(aktorId, profileringEntity)
        }
        secureLog.info("Lagret profilering for bruker med fnr: $fnr")*/
    }

    /**
     * Henter og lagrer arbeidssøkerdata for bruker med aktørId.
     * Med arbeidssøkerdata menes:
     *
     * - Siste arbeidssøkerperiode
     * - Siste opplysninger om arbeidssøker
     * - Siste profilering av bruker
     */
    @Transactional
    fun hentOgLagreArbeidssoekerdataForBruker(aktorId: AktorId) {
        val fnr: Fnr? = pdlIdentRepository.hentAktivFnrForPerson(aktorId.toString())
        if (fnr == null) {
            secureLog.info("Fant ingen fødselsnummer for bruker med aktorId: $aktorId")
            throw RuntimeException("Fant ingen fødselsnummer for bruker")
        }

        hentOgLagreAggregertPeriode(fnr, aktorId)
    }

    fun slettArbeidssoekerData(aktorId: AktorId) {
//        val alleFnrForPerson = pdlIdentRepository.hentFnrIdenterForBruker(aktorId.get()).identer.map { Fnr.of(it) }
//        if (alleFnrForPerson.isEmpty()) {
//            secureLog.warn("Fant ingen fødselsnummer for bruker med aktorId: ${aktorId.get()}, kunne ikke slette arbeidssøkerdata.")
//            return
//        }
//
//        try {
//            sisteArbeidssoekerPeriodeRepository.slettSisteArbeidssoekerPeriode(alleFnrForPerson)
//        } catch (e: Exception) {
//            secureLog.error(
//                "Feil ved sletting av siste arbeidssøkerperiode for bruker med aktorId: ${aktorId.get()}",
//                e
//            )
//            throw e
//        }
    }

    /**
     * Henter aggregert periode fra snapshot-endepunktet og lagrer periode, opplysninger og profilering.
     * Returnerer false uten å endre noe dersom ingen periode finnes, eller perioden ikke er [forventetPeriodeId].
     */
    private fun hentOgLagreAggregertPeriode(fnr: Fnr, aktorId: AktorId?, forventetPeriodeId: UUID? = null): Boolean {
        secureLog.info("Henter aggregert arbeidssøkerperiode for bruker med fnr: $fnr")
        val aggregertPeriode: AggregertPeriodeResponse? =
            arbeidssoekerregisterClient.hentAggregertArbeidssokerPeriode(fnr.get())

        if (aggregertPeriode == null || (forventetPeriodeId != null && aggregertPeriode.id != forventetPeriodeId)) {
            secureLog.info("Fant ingen aggregert arbeidssøkerperiode for bruker med fnr: $fnr, forventet periodeId: $forventetPeriodeId")
            return false
        }

        erstattSisteArbeidssoekerPeriode(fnr, aggregertPeriode.id)
        lagreOpplysningerOgProfilering(aggregertPeriode, fnr, aktorId)
        return true
    }

    private fun erstattSisteArbeidssoekerPeriode(fnr: Fnr, periodeId: UUID) {
//        hentAlleFnrOgSlettGamlePerioder(fnr)
//        sisteArbeidssoekerPeriodeRepository.insertSisteArbeidssoekerPeriode(
//            ArbeidssoekerPeriodeEntity(periodeId, fnr.get())
//        )
//        secureLog.info("Lagret siste arbeidssøkerperiode for bruker med fnr: $fnr")
    }

    private fun lagreOpplysningerOgProfilering(
        aggregertPeriode: AggregertPeriodeResponse,
        fnr: Fnr,
        aktorId: AktorId?
    ) {
//        val opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoekerEntity? =
//            aggregertPeriode.opplysning?.toOpplysningerOmArbeidssoekerEntity(aggregertPeriode.id)
//
//        if (opplysningerOmArbeidssoeker == null) {
//            secureLog.info("Fant ingen opplysninger om arbeidssøker for bruker med fnr: $fnr")
//            return
//        }
//        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(
//            opplysningerOmArbeidssoeker
//        )
//        if (aktorId != null) {
//            opensearchIndexerPaDatafelt.updateOpplysningerOmArbeidssoeker(aktorId, opplysningerOmArbeidssoeker)
//        }
//        secureLog.info("Lagret opplysninger om arbeidssøker for bruker med fnr: $fnr")
//
//        val profilering: ProfileringEntity? = aggregertPeriode.profilering
//            ?.takeIf { it.opplysningerOmArbeidssokerId == opplysningerOmArbeidssoeker.opplysningerOmArbeidssoekerId }
//            ?.toProfileringEntity(aggregertPeriode.id)
//
//        if (profilering == null) {
//            secureLog.info("Fant ingen profilering for bruker med fnr: $fnr")
//            return
//        }
//        profileringRepository.insertProfilering(profilering)
//        if (aktorId != null) {
//            opensearchIndexerPaDatafelt.updateProfilering(aktorId, profilering)
//        }
//        secureLog.info("Lagret profilering for bruker med fnr: $fnr")
    }

    private fun hentAlleFnrOgSlettGamlePerioder(fnr: Fnr) {
//        val alleFnrForPerson = pdlIdentRepository.hentFnrIdenterForBruker(fnr.get()).identer.map { Fnr.of(it) }
//        sisteArbeidssoekerPeriodeRepository.slettSisteArbeidssoekerPeriode(alleFnrForPerson)
    }
}
