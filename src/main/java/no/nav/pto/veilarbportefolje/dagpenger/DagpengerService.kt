package no.nav.pto.veilarbportefolje.dagpenger

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerVedtakResponseDto
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_KILDESYSTEM
import no.nav.pto.veilarbportefolje.ytelserkafka.YtelserKafkaDTO
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*


/**
 * Håndterer behandling av Kafka-meldinger fra [Topic.YTELSER_TOPIC] av typen [YTELSE_TYPE.DAGPENGER].
 * Disse blir routet fra YtelserKafkaService
 *
 * Denne klassen håndterer funksjonalitet knyttet til å starte (les: lagre), oppdatere og stoppe (les: slette)
 * ytelser for dagpenger.
 */
@Service
class DagpengerService(
    val dagpengerClient: DagpengerClient,
    val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    val pdlIdentRepository: PdlIdentRepository,
    val aktorClient: AktorClient,
    val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt,
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(DagpengerService::class.java)

    fun behandleKafkaMeldingLogikk(kafkaMelding: YtelserKafkaDTO) {
        if (kafkaMelding.kildesystem != YTELSE_KILDESYSTEM.DPSAK) {
            logger.warn("Mottok ytelse-melding for Dagpenger med uventet kildesystem : ${kafkaMelding.kildesystem}, forventet DPSAK. Ignorerer melding.")
            return
        }
        val erUnderOppfolging = pdlIdentRepository.erBrukerUnderOppfolging(kafkaMelding.personId)

        if (!erUnderOppfolging) {
            secureLog.info(
                "Bruker {} er ikke under oppfølging, ignorerer dagpenger-ytelse melding.",
                kafkaMelding.personId
            )
            return
        }
        val aktorId = aktorClient.hentAktorId(Fnr.of(kafkaMelding.personId))
        val oppfolgingsStartdato = hentOppfolgingStartdato(aktorId)
        lagreDagpengerForBruker(kafkaMelding.personId, aktorId, oppfolgingsStartdato)
    }

    fun hentOgLagreDagpengerForBrukerVedOppfolgingStart(aktorId: AktorId) {
        val personIdent = aktorClient.hentFnr(aktorId).get()
        lagreDagpengerForBruker(personIdent, aktorId, LocalDate.now())
    }

    fun lagreDagpengerForBruker(
        personIdent: String,
        aktorId: AktorId,
        oppfolgingsStartdato: LocalDate,
    ) {
        val sisteDagpengerVedtak = hentSistePeriodeFraApi(personIdent, oppfolgingsStartdato)

        if (sisteDagpengerVedtak == null) {
            secureLog.info(
                "Ingen Dagpenger-periode funnet i oppfølgingsperioden for bruker {}, ignorerer dagpenger-ytelse melding.",
                personIdent
            )
            return
        }


        // legg til sjekk på om bruker har aktiv ytelse (avhenging av hva slags data vi får fra apiet)
        val harAktivYtelse = sisteDagpengerVedtak.tom.isAfter(LocalDate.now().minusDays(1))

        upsertDagpengerForAktivIdentForBruker(personIdent, sisteDagpengerVedtak)
//        opensearchIndexerPaDatafelt.oppdaterDagpenger(
//            aktorId,
//            harAktivYtelse,
//            sisteDagpengerVedtak.tom,
//            sisteDagpengerVedtak.rettighet
//        )
    }

    // Oppdater logikk avhenging av hvordan vi får dataene fra apiet
    fun hentSistePeriodeFraApi(personIdent: String, oppfolgingsStartdato: LocalDate): DagpengerVedtakResponseDto? {
        val respons = dagpengerClient.hentDagpengerVedtak(personIdent, oppfolgingsStartdato.toString())
        val vedtakIOppfolgingsPeriode = respons
            .filter { vedtak -> vedtak.tom.isAfter(oppfolgingsStartdato.minusDays(1)) }

        return vedtakIOppfolgingsPeriode.maxByOrNull { it.fom }
    }

    fun upsertDagpengerForAktivIdentForBruker(
        personIdent: String,
        sisteDagpengerVedtak: DagpengerVedtakResponseDto
    ) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        if (alleFnrIdenterForBruker.size > 1) {
            alleFnrIdenterForBruker.forEach { ident ->
                //dagpengerRespository.slettDagpengerForBruker(ident)
            }
        }

        //dagpengerRespository.upsertAap(personIdent, sisteDagpengerVedtak)
    }

    fun slettDagpengerData(aktorId: AktorId, maybeFnr: Optional<Fnr>) {
        if (maybeFnr.isEmpty) {
            secureLog.warn(
                "Kunne ikke slette Dagpenger bruker med Aktør-ID ${aktorId.get()}. Årsak fødselsnummer-parameter var tom."
            )
            return
        }

        try {
            slettDagpengerForAlleIdenterForBruker(maybeFnr.get().toString())
        } catch (e: Exception) {
            secureLog.error("Feil ved sletting av Dagpenger data for bruker med fnr: ${maybeFnr.get()}", e)
            return
        }
    }

    fun slettDagpengerForAlleIdenterForBruker(personIdent: String) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        alleFnrIdenterForBruker.forEach { ident ->
            //dagpengerRespository.slettDagpengerForBruker(ident)
        }
    }

    fun hentOppfolgingStartdato(aktorId: AktorId): LocalDate {
        val oppfolgingsdata = oppfolgingRepositoryV2.hentOppfolgingMedStartdato(aktorId)
            .orElseThrow { IllegalStateException("Ingen oppfølgingsdata funnet") }

        if (oppfolgingsdata.oppfolging && oppfolgingsdata.startDato != null) {
            return toLocalDateOrNull(oppfolgingsdata.startDato)
        }

        secureLog.info("Fant ikke oppfolgingsdata for bruker med aktorId {}", aktorId)
        throw IllegalStateException("Bruker er ikke under oppfølging")
    }

}
