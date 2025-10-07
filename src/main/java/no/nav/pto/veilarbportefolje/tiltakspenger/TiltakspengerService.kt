package no.nav.pto.veilarbportefolje.tiltakspenger

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aap.domene.YTELSE_MELDINGSTYPE
import no.nav.pto.veilarbportefolje.aap.domene.YTELSE_TYPE
import no.nav.pto.veilarbportefolje.aap.domene.YtelserKafkaDTO
import no.nav.pto.veilarbportefolje.domene.AktorClient
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerResponseDto
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDate
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*


/**
 * Håndterer behandling av Kafka-meldinger fra [Topic.YTELSER_TOPIC] av typen [YTELSE_TYPE.TILTAKSPENGER].
 * Disse blir routet fra YtelserKafkaService
 *
 * Denne klassen håndterer funksjonalitet knyttet til å starte (les: lagre), oppdatere og stoppe (les: slette)
 * ytelser for tiltakspenger.
 */
@Service
class TiltakspengerService(
    val tiltakspengerClient: TiltakspengerClient,
    val tiltakspengerRespository: TiltakspengerRespository,
    val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    val pdlIdentRepository: PdlIdentRepository,
    val aktorClient: AktorClient
) {

    fun behandleKafkaMeldingLogikk(kafkaMelding: YtelserKafkaDTO) {
        val erUnderOppfolging = pdlIdentRepository.erBrukerUnderOppfolging(kafkaMelding.personId)

        if (!erUnderOppfolging) {
            secureLog.info("Bruker {} er ikke under oppfølging, ignorerer tiltakspenger-ytelse melding.", kafkaMelding.personId)
            return
        }
        val aktorId = aktorClient.hentAktorId(Fnr.of(kafkaMelding.personId))
        val oppfolgingsStartdato = hentOppfolgingStartdato(aktorId)
        lagreTiltakspengerForBruker(kafkaMelding.personId, aktorId, oppfolgingsStartdato, kafkaMelding.meldingstype)
    }

    fun hentOgLagreTiltakspengerForBrukerVedBatchjobb(aktorId: AktorId) {
        val personIdent = aktorClient.hentFnr(aktorId).get()
        val oppfolgingsStartdato = hentOppfolgingStartdato(aktorId)
        lagreTiltakspengerForBruker(personIdent, aktorId, oppfolgingsStartdato, YTELSE_MELDINGSTYPE.OPPDATER)
    }

    fun hentOgLagreTiltakspengerForBrukerVedOppfolgingStart(aktorId: AktorId) {
        val personIdent = aktorClient.hentFnr(aktorId).get()
        lagreTiltakspengerForBruker(personIdent, aktorId, LocalDate.now(), YTELSE_MELDINGSTYPE.OPPRETT)
    }

    fun lagreTiltakspengerForBruker(
        personIdent: String,
        aktorId: AktorId,
        oppfolgingsStartdato: LocalDate,
        meldingstype: YTELSE_MELDINGSTYPE
    ) {
        val sisteTiltakspengerVedtak = hentSistePeriodeFraApi(personIdent, oppfolgingsStartdato)

        if (sisteTiltakspengerVedtak == null)
            if (meldingstype == YTELSE_MELDINGSTYPE.OPPDATER) {
                secureLog.info(
                    "Ingen Tiltakspenger-periode funnet i oppfølgingsperioden for bruker {}, " +
                            "sletter eventuell eksisterende perioder i databasen", personIdent
                )
                slettTiltakspengerForAlleIdenterForBruker(personIdent)
                return
            } else {
                secureLog.info(
                    "Ingen Tiltakspenger-periode funnet i oppfølgingsperioden for bruker {}, ignorerer tiltakspenger-ytelse melding.",
                    personIdent
                )
                return
            }

        upsertTiltakspengerForAktivIdentForBruker(personIdent, aktorId, sisteTiltakspengerVedtak)
    }

    fun hentSistePeriodeFraApi(personIdent: String, oppfolgingsStartdato: LocalDate): TiltakspengerResponseDto? {
        val respons = tiltakspengerClient.hentTiltakspenger(personIdent, oppfolgingsStartdato.toString())
        val vedtakIOppfolgingsPeriode = respons
            .filter { vedtak -> vedtak.tom.isAfter(oppfolgingsStartdato.minusDays(1)) }

        return vedtakIOppfolgingsPeriode.maxByOrNull { it.fom }
    }

    fun upsertTiltakspengerForAktivIdentForBruker(personIdent: String, aktorId: AktorId, sisteTiltakspengerVedtak: TiltakspengerResponseDto) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        if (alleFnrIdenterForBruker.size > 1) {
            alleFnrIdenterForBruker.forEach { ident ->
                tiltakspengerRespository.slettTiltakspengerForBruker(ident)
            }
        }

        tiltakspengerRespository.upsertAap(personIdent, sisteTiltakspengerVedtak )
    }

    fun slettTiltakspengerData(aktorId: AktorId, maybeFnr: Optional<Fnr>) {
        if (maybeFnr.isEmpty) {
            secureLog.warn(
                "Kunne ikke slette Tiltakspenger bruker med Aktør-ID {}. Årsak fødselsnummer-parameter var tom.",
                aktorId.get()
            )
            return
        }

        try {
            slettTiltakspengerForAlleIdenterForBruker(maybeFnr.get().toString())
        } catch (e: Exception) {
            secureLog.error("Feil ved sletting av Tiltakspenger data for bruker med fnr: ${maybeFnr.get()}", e)
            return
        }
    }

    fun slettTiltakspengerForAlleIdenterForBruker(personIdent: String) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        alleFnrIdenterForBruker.forEach { ident ->
            tiltakspengerRespository.slettTiltakspengerForBruker(ident)
        }
    }

    fun hentOppfolgingStartdato(aktorId: AktorId): LocalDate {
        val oppfolgingsdata = oppfolgingRepositoryV2.hentOppfolgingMedStartdato(aktorId)
            .orElseThrow { IllegalStateException("Ingen oppfølgingsdata funnet") }

        if (oppfolgingsdata.oppfolging && oppfolgingsdata.startDato != null) {
            return toLocalDate(oppfolgingsdata.startDato)
        }

        secureLog.info("Fant ikke oppfolgingsdata for bruker med aktorId {}", aktorId)
        throw IllegalStateException("Bruker er ike under oppfølging")
    }

}
