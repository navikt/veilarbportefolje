package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.siste14aVedtak.Siste14aVedtakRepository
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker as OpplysningerOmArbeidssoekerKafkaMelding


@Service
class ArbeidssoekerPeriodeKafkaMeldingService(
    private val arbeidssoekerService: ArbeidssoekerService
) : KafkaCommonConsumerService<Periode>() {
    override fun behandleKafkaMeldingLogikk(kafkaMelding: Periode) {
        arbeidssoekerService.behandleKafkaMeldingLogikk(kafkaMelding)
    }
}

@Service
class ArbeidssoekerOpplysningerOmArbeidssoekerKafkaMeldingService(
    private val arbeidssoekerService: ArbeidssoekerService
) : KafkaCommonConsumerService<OpplysningerOmArbeidssoekerKafkaMelding>() {
    override fun behandleKafkaMeldingLogikk(kafkaMelding: OpplysningerOmArbeidssoekerKafkaMelding) {
        arbeidssoekerService.behandleKafkaMeldingLogikk(kafkaMelding)
    }
}

@Service
class ArbeidssoekerService(
    private val oppslagArbeidssoekerregisteretClient: OppslagArbeidssoekerregisteretClient,
    private val pdlIdentRepository: PdlIdentRepository,
    private val opplysningerOmArbeidssoekerRepository: OpplysningerOmArbeidssoekerRepository,
    private val sisteArbeidssoekerPeriodeRepository: SisteArbeidssoekerPeriodeRepository,
    private val profileringRepository: ProfileringRepository,
    private val siste14aVedtakRepository: Siste14aVedtakRepository
) {

    @Transactional
    fun behandleKafkaMeldingLogikk(kafkaMelding: Periode) {
        val periodeId = kafkaMelding.id
        val identitetsnummer = kafkaMelding.identitetsnummer

        val fnr: Fnr = Fnr.ofValidFnr(identitetsnummer.toString())

        if (!pdlIdentRepository.erBrukerUnderOppfolging(fnr.get())) {
            secureLog.info("Bruker er ikke under oppfølging, ignorerer endring på bruker med fnr: {}", fnr.get())
            return
        }

        secureLog.info("Behandler endring på arbeidssøkerpeiode for bruker med fnr: $fnr")
        sisteArbeidssoekerPeriodeRepository.slettSisteArbeidssoekerPeriode(fnr)
        sisteArbeidssoekerPeriodeRepository.insertSisteArbeidssoekerPeriode(fnr, periodeId)
        secureLog.info("Lagret siste arbeidssøkerperiode for bruker med fnr: $fnr")

        val opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker? =
            hentSisteOpplysningerOmArbeidssoeker(fnr, periodeId)?.toOpplysningerOmArbeidssoeker()
        if (opplysningerOmArbeidssoeker == null) {
            secureLog.info("Fant ingen opplysninger om arbeidssøker for bruker med fnr: $fnr")
            return
        }
        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(
            opplysningerOmArbeidssoeker
        )
        secureLog.info("Lagret opplysninger om arbeidssøker for bruker med fnr: $fnr")

        val identerForBruker = pdlIdentRepository.hentIdenterForBruker(fnr.get())
        val har14aVedtak = siste14aVedtakRepository.hentSiste14aVedtak(identerForBruker).isPresent

        if (!har14aVedtak) {
            val profilering: Profilering? = hentSisteProfilering(
                fnr,
                periodeId,
                opplysningerOmArbeidssoeker.opplysningerOmArbeidssoekerId
            )?.toProfilering()

            if (profilering == null) {
                secureLog.info("Fant ingen profilering for bruker med fnr: $fnr")
                return
            }

            profileringRepository.insertProfilering(profilering)
            secureLog.info("Lagret profilering for bruker med fnr: $fnr")
        }
    }

    fun behandleKafkaMeldingLogikk(opplysninger: OpplysningerOmArbeidssoekerKafkaMelding) {
        val arbeidssoekerPeriodeId = opplysninger.periodeId
        val opplysningerOmArbeidssoekerId = opplysninger.id

        val sisteArbeidssoekerPeriode =
            sisteArbeidssoekerPeriodeRepository.hentSisteArbeidssoekerPeriode(arbeidssoekerPeriodeId)

        if (sisteArbeidssoekerPeriode == null) {
            secureLog.info("Ingen arbeidssøkerperiode lagret med arbeidssoekerPeriodeId: $arbeidssoekerPeriodeId")
            return
        }

        val fnr = sisteArbeidssoekerPeriode.fnr
        if (!pdlIdentRepository.erBrukerUnderOppfolging(fnr.get())) {
            secureLog.info(
                "Bruker med fnr ${fnr.get()} er ikke under oppfølging, men har arbeidssøkerpeiode lagret. " +
                        "Dette betyr at arbeidssøkerdata ikke har blitt slettet riktig når bruker gikk ut av oppfølging. " +
                        "Ignorer melding, data må slettes manuelt og slettelogikk ved utgang av oppfølging bør kontrollsjekkes for feil."
            )
            return
        }

        val opplysningerOmArbeidssoeker = opplysningerOmArbeidssoekerRepository.harSisteOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoekerId)
        if (opplysningerOmArbeidssoeker) {
            secureLog.info("Opplysninger om arbeidssøker allerede lagret for bruker med fnr: $fnr")
            return
        }

        opplysningerOmArbeidssoekerRepository.slettOpplysningerOmArbeidssoeker(sisteArbeidssoekerPeriode.arbeidssoekerperiodeId)
        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(opplysninger.toOpplysningerOmArbeidssoeker())
        secureLog.info("Lagret opplysninger om arbeidssøker for bruker med fnr: $fnr")
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
        val fnr: Fnr? = pdlIdentRepository.hentFnrForAktivBruker(aktorId)
        if (fnr == null) {
            secureLog.info("Fant ingen fødselsnummer for bruker med aktorId: $aktorId")
            throw RuntimeException("Fant ingen fødselsnummer for bruker")
        }

        secureLog.info("Henter arbeidssøkerperioder for bruker med fnr: $fnr")
        val aktivArbeidssoekerperiode: ArbeidssokerperiodeResponse? =
            oppslagArbeidssoekerregisteretClient.hentArbeidssokerPerioder(fnr.get())
                ?.find { it.avsluttet == null }

        if (aktivArbeidssoekerperiode == null) {
            secureLog.info("Fant ingen aktiv arbeidssøkerperiode for bruker med fnr: $fnr")
            return
        }

        sisteArbeidssoekerPeriodeRepository.insertSisteArbeidssoekerPeriode(fnr, aktivArbeidssoekerperiode.periodeId)
        secureLog.info("Lagret siste arbeidssøkerperiode for bruker med fnr: $fnr")

        val sisteOpplysningerOmArbeidssoeker =
            hentSisteOpplysningerOmArbeidssoeker(fnr, aktivArbeidssoekerperiode.periodeId)

        secureLog.info("Henter opplysninger om arbeidssøker for bruker med fnr: $fnr")
        if (sisteOpplysningerOmArbeidssoeker == null) {
            secureLog.info("Fant ingen opplysninger om arbeidssøker for bruker med fnr: $fnr")
            return
        }

        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(
            sisteOpplysningerOmArbeidssoeker.toOpplysningerOmArbeidssoeker()
        )
        secureLog.info("Lagret opplysninger om arbeidssøker for bruker med fnr: $fnr")

        val sisteProfilering: ProfileringResponse? =
            hentSisteProfilering(
                fnr,
                aktivArbeidssoekerperiode.periodeId,
                sisteOpplysningerOmArbeidssoeker.opplysningerOmArbeidssoekerId
            )

        if (sisteProfilering == null) {
            secureLog.info("Fant ingen profilering for bruker med fnr: $fnr")
            return
        }

        profileringRepository.insertProfilering(sisteProfilering.toProfilering())
        secureLog.info("Lagret profilering for bruker med fnr: $fnr")
    }

    fun slettArbeidssoekerData(aktorId: AktorId, maybeFnr: Optional<Fnr>) {
        if (maybeFnr.isEmpty) {
            secureLog.warn(
                "Kunne ikke slette oppfolgingsbruker med Aktør-ID {}. Årsak fødselsnummer-parameter var tom.",
                aktorId.get()
            )
            return
        }

        sisteArbeidssoekerPeriodeRepository.slettSisteArbeidssoekerPeriode(maybeFnr.get())
    }

    private fun hentSisteProfilering(
        fnr: Fnr,
        arbeidssoekerPeriodeId: UUID,
        opplysningerOmArbeidssoekerId: UUID
    ): ProfileringResponse? {
        secureLog.info("Henter profilering for bruker med fnr: $fnr")
        val sisteProfilering: ProfileringResponse? =
            oppslagArbeidssoekerregisteretClient.hentProfilering(fnr.get(), arbeidssoekerPeriodeId)
                ?.filter { it.opplysningerOmArbeidssoekerId == opplysningerOmArbeidssoekerId }
                ?.maxByOrNull { it.sendtInnAv.tidspunkt }
        return sisteProfilering
    }

    private fun hentSisteOpplysningerOmArbeidssoeker(
        fnr: Fnr,
        periodeId: UUID
    ): OpplysningerOmArbeidssoekerResponse? {
        val opplysningerOmArbeidssoeker: List<OpplysningerOmArbeidssoekerResponse>? =
            oppslagArbeidssoekerregisteretClient.hentOpplysningerOmArbeidssoeker(
                fnr.get(),
                periodeId
            )
        val sisteOpplysningerOmArbeidssoeker = opplysningerOmArbeidssoeker?.maxByOrNull {
            it.sendtInnAv.tidspunkt
        }
        return sisteOpplysningerOmArbeidssoeker
    }
}