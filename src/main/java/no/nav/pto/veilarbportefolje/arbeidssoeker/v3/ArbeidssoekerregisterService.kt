package no.nav.pto.veilarbportefolje.arbeidssoeker.v3

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as PeriodeKafkaMelding
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
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

    // Fra admin controller og oppfølging start
    @Transactional
    fun hentOgLagreArbeidssoekerdataForBruker(aktorId: AktorId) {
        val fnr: Fnr? = pdlIdentRepository.hentAktivFnrForPerson(aktorId.toString())
        if (fnr == null) {
            secureLog.info("Fant ingen fødselsnummer for bruker med aktorId: $aktorId")
            throw RuntimeException("Fant ingen fødselsnummer for bruker")
        }

        hentOgLagreAggregertPeriode(fnr, aktorId)
    }

    private fun hentOgLagreAggregertPeriode(fnr: Fnr, aktorId: AktorId?, forventetPeriodeId: UUID? = null): Boolean {
        secureLog.info("Henter aggregert arbeidssøkerperiode for bruker med fnr: $fnr")
        val aggregertPeriode: AggregertPeriodeResponse? =
            arbeidssoekerregisterClient.hentAggregertArbeidssokerPeriode(fnr.get())

        if (aggregertPeriode == null || (forventetPeriodeId != null && aggregertPeriode.id != forventetPeriodeId)) {
            secureLog.info("Fant ingen aggregert arbeidssøkerperiode for bruker med fnr: $fnr, forventet periodeId: $forventetPeriodeId")
            return false
        }

        //erstattSisteArbeidssoekerPeriode(fnr, aggregertPeriode.id)
        //lagreOpplysningerOgProfilering(aggregertPeriode, fnr, aktorId)
        return true
    }

}
