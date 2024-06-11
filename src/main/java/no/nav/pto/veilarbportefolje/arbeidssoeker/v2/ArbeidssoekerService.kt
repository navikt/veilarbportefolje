package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import io.getunleash.DefaultUnleash
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.pto.veilarbportefolje.config.FeatureToggle
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering as ProfileringKafkaMelding
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
class ArbeidssoekerProfileringKafkaMeldingService(
    private val arbeidssoekerService: ArbeidssoekerService
) : KafkaCommonConsumerService<ProfileringKafkaMelding>() {
    override fun behandleKafkaMeldingLogikk(kafkaMelding: ProfileringKafkaMelding) {
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
    private val defaultUnleash: DefaultUnleash,
    private val arbeidssoekerDataRepository: ArbeidssoekerDataRepository,
    private val opensearchIndexerV2: OpensearchIndexerV2
) {

    @Transactional
    fun behandleKafkaMeldingLogikk(kafkaMelding: Periode) {
        if (!FeatureToggle.brukNyttArbeidssoekerregisterKafka(defaultUnleash)) {
            secureLog.info("Bryter for å lytte på kafkameldinger fra nytt arbeidssøkerregister er skrudd av. Ignorerer melding.")
            return
        }
        val periodeId = kafkaMelding.id
        val identitetsnummer = kafkaMelding.identitetsnummer

        val fnr: Fnr = Fnr.ofValidFnr(identitetsnummer.toString())

        secureLog.info("Behandler endring på arbeidssøkerperiode for bruker med fnr: $fnr, periodeId: $periodeId")

        if (!pdlIdentRepository.erBrukerUnderOppfolging(fnr.get())) {
            secureLog.info("Bruker er ikke under oppfølging, ignorerer endring på bruker med fnr: {}", fnr.get())
            return
        }

        val aktorId = pdlIdentRepository.hentAktorIdForAktivBruker(fnr)

        sisteArbeidssoekerPeriodeRepository.slettSisteArbeidssoekerPeriode(fnr)
        sisteArbeidssoekerPeriodeRepository.insertSisteArbeidssoekerPeriode(ArbeidssoekerPeriodeEntity(periodeId, fnr.get()))
        secureLog.info("Lagret siste arbeidssøkerperiode for bruker med fnr: $fnr")

        val opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoekerEntity? =
            hentSisteOpplysningerOmArbeidssoeker(fnr, periodeId)?.toOpplysningerOmArbeidssoekerEntity()
        if (opplysningerOmArbeidssoeker == null) {
            secureLog.info("Fant ingen opplysninger om arbeidssøker for bruker med fnr: $fnr")
            return
        }
        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(
            opplysningerOmArbeidssoeker
        )
        if (aktorId != null) {
            opensearchIndexerV2.updateOpplysningerOmArbeidssoeker(aktorId,opplysningerOmArbeidssoeker)
        }

        secureLog.info("Lagret opplysninger om arbeidssøker for bruker med fnr: $fnr")


        val profilering: ProfileringEntity? = hentSisteProfilering(
            fnr,
            periodeId,
            opplysningerOmArbeidssoeker.opplysningerOmArbeidssoekerId
        )?.toProfileringEntity()

        if (profilering == null) {
            secureLog.info("Fant ingen profilering for bruker med fnr: $fnr")
            return
        }

        profileringRepository.insertProfilering(profilering)
        if (aktorId != null) {
            opensearchIndexerV2.updateProfilering(aktorId, profilering)
        }
        secureLog.info("Lagret profilering for bruker med fnr: $fnr")
    }

    @Transactional
    fun behandleKafkaMeldingLogikk(opplysninger: OpplysningerOmArbeidssoekerKafkaMelding) {
        if (!FeatureToggle.brukNyttArbeidssoekerregisterKafka(defaultUnleash)) {
            secureLog.info("Bryter for å lytte på kafkameldinger fra nytt arbeidssøkerregister er skrudd av. Ignorerer melding.")
            return
        }
        val arbeidssoekerPeriodeId = opplysninger.periodeId
        val opplysningerOmArbeidssoekerId = opplysninger.id

        secureLog.info("Behandler endring på opplysninger om arbeidssøker for bruker med arbeidssoekerPeriodeId: $arbeidssoekerPeriodeId, opplysningerOmArbeidssoekerId: $opplysningerOmArbeidssoekerId")

        val sisteArbeidssoekerPeriode =
            sisteArbeidssoekerPeriodeRepository.hentSisteArbeidssoekerPeriode(arbeidssoekerPeriodeId)

        if (sisteArbeidssoekerPeriode == null) {
            secureLog.info("Ingen arbeidssøkerperiode lagret med arbeidssoekerPeriodeId: $arbeidssoekerPeriodeId")
            return
        }

        val fnr = sisteArbeidssoekerPeriode.fnr
        if (!pdlIdentRepository.erBrukerUnderOppfolging(fnr)) {
            secureLog.info(
                "Bruker med fnr ${fnr} er ikke under oppfølging, men har arbeidssøkerpeiode lagret. " +
                        "Dette betyr at arbeidssøkerdata ikke har blitt slettet riktig når bruker gikk ut av oppfølging. " +
                        "Ignorer melding, data må slettes manuelt og slettelogikk ved utgang av oppfølging bør kontrollsjekkes for feil."
            )
            return
        }

        val aktorId = pdlIdentRepository.hentAktorIdForAktivBruker(Fnr.of(fnr))

        val opplysningerOmArbeidssoeker =
            opplysningerOmArbeidssoekerRepository.harSisteOpplysningerOmArbeidssoeker(opplysningerOmArbeidssoekerId)
        if (opplysningerOmArbeidssoeker) {
            secureLog.info("Opplysninger om arbeidssøker allerede lagret for bruker med fnr: $fnr")
            return
        }

        val opplysningerOmArbeidssoekerEntity = opplysninger.toOpplysningerOmArbeidssoekerEntity()
        opplysningerOmArbeidssoekerRepository.slettOpplysningerOmArbeidssoeker(sisteArbeidssoekerPeriode.arbeidssoekerperiodeId)
        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(opplysningerOmArbeidssoekerEntity)

        if (aktorId != null) {
            opensearchIndexerV2.updateOpplysningerOmArbeidssoeker(aktorId, opplysningerOmArbeidssoekerEntity)
        }

        secureLog.info("Lagret opplysninger om arbeidssøker for bruker med fnr: $fnr")
    }

    @Transactional
    fun behandleKafkaMeldingLogikk(kafkaMelding: ProfileringKafkaMelding) {
        if (!FeatureToggle.brukNyttArbeidssoekerregisterKafka(defaultUnleash)) {
            secureLog.info("Bryter for å lytte på kafkameldinger fra nytt arbeidssøkerregister er skrudd av. Ignorerer melding.")
            return
        }
        secureLog.info("Behandler endring på profilering for bruker med arbeidssoekerPeriodeId: ${kafkaMelding.periodeId}")

        val sisteArbeidssoekerPeriode =
            sisteArbeidssoekerPeriodeRepository.hentSisteArbeidssoekerPeriode(kafkaMelding.periodeId)

        if (sisteArbeidssoekerPeriode == null) {
            secureLog.info("Ingen arbeidssøkerperiode lagret med arbeidssoekerPeriodeId: ${kafkaMelding.periodeId}")
            return
        }

        val fnr = sisteArbeidssoekerPeriode.fnr
        if (!pdlIdentRepository.erBrukerUnderOppfolging(fnr)) {
            secureLog.info(
                "Bruker med fnr ${fnr} er ikke under oppfølging, men har arbeidssøkerpeiode lagret. " +
                        "Dette betyr at arbeidssøkerdata ikke har blitt slettet riktig når bruker gikk ut av oppfølging. " +
                        "Ignorer melding, data må slettes manuelt og slettelogikk ved utgang av oppfølging bør kontrollsjekkes for feil."
            )
            return
        }
        val aktorId = pdlIdentRepository.hentAktorIdForAktivBruker(Fnr.of(fnr))
        val profileringEntity = kafkaMelding.toProfileringEntity()

        profileringRepository.insertProfilering(profileringEntity)
        if (aktorId != null) {
            opensearchIndexerV2.updateProfilering(aktorId, profileringEntity)
        }
        secureLog.info("Lagret profilering for bruker med fnr: $fnr")
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

        sisteArbeidssoekerPeriodeRepository.slettSisteArbeidssoekerPeriode(fnr)
        sisteArbeidssoekerPeriodeRepository.insertSisteArbeidssoekerPeriode(ArbeidssoekerPeriodeEntity(aktivArbeidssoekerperiode.periodeId, fnr.get()))
        secureLog.info("Lagret siste arbeidssøkerperiode for bruker med fnr: $fnr")

        val sisteOpplysningerOmArbeidssoeker =
            hentSisteOpplysningerOmArbeidssoeker(fnr, aktivArbeidssoekerperiode.periodeId)

        secureLog.info("Henter opplysninger om arbeidssøker for bruker med fnr: $fnr")
        if (sisteOpplysningerOmArbeidssoeker == null) {
            secureLog.info("Fant ingen opplysninger om arbeidssøker for bruker med fnr: $fnr")
            return
        }

        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(
            sisteOpplysningerOmArbeidssoeker.toOpplysningerOmArbeidssoekerEntity()
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

        profileringRepository.insertProfilering(sisteProfilering.toProfileringEntity())
        secureLog.info("Lagret profilering for bruker med fnr: $fnr")
    }

    fun hentArbeidssoekerData(fnrList: List<Fnr>): List<ArbeidssoekerData> {
        val opplysningerOmArbeidssoekerPaaBrukere = arbeidssoekerDataRepository.hentOpplysningerOmArbeidssoeker(fnrList)
        val profileringPaaBrukere = arbeidssoekerDataRepository.hentProfileringsresultatListe(fnrList)

        return opplysningerOmArbeidssoekerPaaBrukere.map { (fnr, opplysning) ->
            ArbeidssoekerData(
                fnr = Fnr.of(fnr),
                opplysningerOmArbeidssoeker = opplysning,
                profilering = profileringPaaBrukere[fnr]
            )
        }
    }

    fun slettArbeidssoekerData(aktorId: AktorId, maybeFnr: Optional<Fnr>) {
        if (maybeFnr.isEmpty) {
            secureLog.warn(
                "Kunne ikke slette oppfolgingsbruker med Aktør-ID {}. Årsak fødselsnummer-parameter var tom.",
                aktorId.get()
            )
            return
        }

        try {
            sisteArbeidssoekerPeriodeRepository.slettSisteArbeidssoekerPeriode(maybeFnr.get())
        } catch (e: Exception) {
            secureLog.error("Feil ved sletting av siste arbeidssøkerperiode for bruker med fnr: ${maybeFnr.get()}", e)
            return
        }
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