package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ArbeidssoekerService(
    private val oppslagArbeidssoekerregisteretClient: OppslagArbeidssoekerregisteretClient,
    private val pdlIdentRepository: PdlIdentRepository,
    private val opplysningerOmArbeidssoekerRepository: OpplysningerOmArbeidssoekerRepository,
    private val sisteArbeidssoekerPeriodeRepository: SisteArbeidssoekerPeriodeRepository,
    private val profileringRepository: ProfileringRepository,
) {
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

        val opplysningerOmArbeidssoeker: List<OpplysningerOmArbeidssoekerResponse>? =
            oppslagArbeidssoekerregisteretClient.hentOpplysningerOmArbeidssoeker(
                fnr.get(),
                aktivArbeidssoekerperiode.periodeId
            )
        val sisteOpplysningerOmArbeidssoeker = opplysningerOmArbeidssoeker?.maxByOrNull {
            it.sendtInnAv.tidspunkt
        }

        secureLog.info("Henter opplysninger om arbeidssøker for bruker med fnr: $fnr")
        if (sisteOpplysningerOmArbeidssoeker == null) {
            secureLog.info("Fant ingen opplysninger om arbeidssøker for bruker med fnr: $fnr")
            return
        }

        opplysningerOmArbeidssoekerRepository.insertOpplysningerOmArbeidssoekerOgJobbsituasjon(sisteOpplysningerOmArbeidssoeker.toOpplysningerOmArbeidssoeker())
        secureLog.info("Lagret opplysninger om arbeidssøker for bruker med fnr: $fnr")

        secureLog.info("Henter opplysninger om arbeidssøker for bruker med fnr: $fnr")
        val sisteProfilering: ProfileringResponse? =
            oppslagArbeidssoekerregisteretClient.hentProfilering(fnr.get(), aktivArbeidssoekerperiode.periodeId)
                ?.filter { it.opplysningerOmArbeidssoekerId == sisteOpplysningerOmArbeidssoeker.opplysningerOmArbeidssoekerId }
                ?.maxByOrNull { it.sendtInnAv.tidspunkt }

        if(sisteProfilering == null) {
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
}