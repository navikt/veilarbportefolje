package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service

@Service
class ArbeidssoekerService(
    private val oppslagArbeidssoekerregisteretClient: OppslagArbeidssoekerregisteretClient,
    private val pdlIdentRepository: PdlIdentRepository,
    private val opplysningerOmArbeidssoekerRepository : OpplysningerOmArbeidssoekerRepository,
) {
    fun hentOgLagreSisteArbeidssoekerPeriodeForBruker(aktorId: AktorId) {
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

        val opplysningerOmArbeidssoeker: List<OpplysningerOmArbeidssoekerResponse>? =
            oppslagArbeidssoekerregisteretClient.hentOpplysningerOmArbeidssoeker(fnr.get(), aktivArbeidssoekerperiode.periodeId)
        val sisteOpplysningerOmArbeidssoeker = opplysningerOmArbeidssoeker?.maxByOrNull {
            it.sendtInnAv.tidspunkt
        }

        secureLog.info("Henter opplysninger om arbeidssøker for bruker med fnr: $fnr")
        if(sisteOpplysningerOmArbeidssoeker == null) {
            secureLog.info("Fant ingen opplysninger om arbeidssøker for bruker med fnr: $fnr")
            return
        }

        opplysningerOmArbeidssoekerRepository.upsertOpplysningerOmArbeidssoeker(sisteOpplysningerOmArbeidssoeker.toOpplysningerOmArbeidssoeker())
        secureLog.info("Lagret opplysninger om arbeidssøker for bruker med fnr: $fnr")

    }
}