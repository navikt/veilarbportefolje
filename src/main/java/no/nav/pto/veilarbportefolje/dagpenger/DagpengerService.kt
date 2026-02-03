package no.nav.pto.veilarbportefolje.dagpenger

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerBeregningerResponseDto
import no.nav.pto.veilarbportefolje.dagpenger.dto.DagpengerPeriodeDto
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_KILDESYSTEM
import no.nav.pto.veilarbportefolje.ytelserkafka.YtelserKafkaDTO
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    val dagpengerRespository: DagpengerRepository,
    val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    val pdlIdentRepository: PdlIdentRepository,
    val aktorClient: AktorClient,
    val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt,
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(DagpengerService::class.java)

    @Transactional
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
        val sisteDagpengerPeriode = hentSistePeriodeFraApi(personIdent, oppfolgingsStartdato)

        if (sisteDagpengerPeriode == null) {
            secureLog.info(
                "Ingen Dagpenger-periode funnet i oppfølgingsperioden for bruker {}, ignorerer dagpenger-ytelse melding.",
                personIdent
            )
            return
        }

        val harAktivYtelse = sisteDagpengerPeriode.tilOgMedDato == null || sisteDagpengerPeriode.tilOgMedDato.isAfter(
            LocalDate.now().minusDays(1)
        )

        val antallResterendeDager = hentAntallResterendeDagerFraApi(personIdent, oppfolgingsStartdato)

        upsertDagpengerForAktivIdentForBruker(personIdent, sisteDagpengerPeriode, antallResterendeDager)
        opensearchIndexerPaDatafelt.oppdaterDagpenger(
            aktorId,
            harAktivYtelse,
            sisteDagpengerPeriode.ytelseType,
            antallResterendeDager?.gjenståendeDager,
            antallResterendeDager?.dato,
            sisteDagpengerPeriode.tilOgMedDato
        )
    }

    fun hentSistePeriodeFraApi(personIdent: String, oppfolgingsStartdato: LocalDate): DagpengerPeriodeDto? {
        val respons = dagpengerClient.hentDagpengerPerioder(personIdent, oppfolgingsStartdato.toString())
        val perioderIDPSAK = respons.perioder.filter { vedtak -> vedtak.kilde == "DP_SAK" }

        val perioderIOppfolgingsPeriode = perioderIDPSAK
            .filter { vedtak ->
                vedtak.tilOgMedDato == null || vedtak.tilOgMedDato.isAfter(
                    oppfolgingsStartdato.minusDays(
                        1
                    )
                )
            }

        //Hvis tilOgMedDato er null er ytelsen fortsatt aktiv og skal prioriteres hvis fraOgMedDatoer er like
        val nyestePeriodeMedDagpenger = perioderIOppfolgingsPeriode.maxWithOrNull(
            compareBy<DagpengerPeriodeDto> { it.fraOgMedDato }
                .thenBy { it.tilOgMedDato == null }
                .thenBy { it.tilOgMedDato }
        )
        return nyestePeriodeMedDagpenger
    }

    fun hentAntallResterendeDagerFraApi(
        personIdent: String,
        oppfolgingsStartdato: LocalDate
    ): DagpengerBeregningerResponseDto? {
        val respons = dagpengerClient.hentDagpengerBeregninger(personIdent, oppfolgingsStartdato.toString())
        return respons.maxByOrNull { it.dato }
    }

    fun upsertDagpengerForAktivIdentForBruker(
        personIdent: String,
        sisteDagpengerPeriode: DagpengerPeriodeDto,
        antallResterendeDager: DagpengerBeregningerResponseDto?,
    ) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        if (alleFnrIdenterForBruker.size > 1) {
            alleFnrIdenterForBruker.forEach { ident ->
                dagpengerRespository.slettDagpengerForBruker(ident)
            }
        }

        dagpengerRespository.upsertDagpengerPerioder(personIdent, sisteDagpengerPeriode, antallResterendeDager)
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
            dagpengerRespository.slettDagpengerForBruker(ident)
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
