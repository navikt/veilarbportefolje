package no.nav.pto.veilarbportefolje.aap

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakResponseDto
import no.nav.pto.veilarbportefolje.aap.domene.YTELSE_TYPE
import no.nav.pto.veilarbportefolje.aap.domene.YtelserKafkaDTO
import no.nav.pto.veilarbportefolje.aap.repository.AapRepository
import no.nav.pto.veilarbportefolje.aap.repository.AapVedtakPeriode
import no.nav.pto.veilarbportefolje.domene.AktorClient
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate


/**
 * Håndterer behandling av Kafka-meldinger fra [Topic.YTELSER_TOPIC] av typen [YTELSE_TYPE.AAP].
 * Disse blir routet fra YtelserKafkaService
 *
 * Denne klassen håndterer funksjonalitet knyttet til å starte (les: lagre), oppdatere og stoppe (les: slette)
 * ytelser for AAP.
 */
@Service
class AapService(
    val aapClient: AapClient,
    val aktorClient: AktorClient,
    val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    val pdlIdentRepository: PdlIdentRepository,
    val aapRepository: AapRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(AapService::class.java)

    @Transactional
    fun behandleKafkaMeldingLogikk(kafkaMelding: YtelserKafkaDTO) {
        val erUnderOppfolging = pdlIdentRepository.erBrukerUnderOppfolging(kafkaMelding.personident)

        if (!erUnderOppfolging) {
            logger.info("Bruker er ikke under oppfølging, ignorerer aap-ytelse melding.")
            return
        }

        val sisteAapPeriode = hentSisteAapPeriodeFraApi(kafkaMelding.personident)

        //todo håndtere tilfeller hvor denne er null, men vi har data i db fra før
        if (sisteAapPeriode == null) {
            logger.info("Ingen AAP-periode funnet i oppfølgingsperioden")
            return
        }

        aapRepository.upsertAap(kafkaMelding.personident, sisteAapPeriode)
    }

    fun hentSisteAapPeriodeFraApi(personIdent: String): AapVedtakResponseDto.Vedtak? {
        val aktorId: AktorId = aktorClient.hentAktorId(Fnr.of(personIdent))
        val oppfolgingsStartdato = hentOppfolgingStartdato(aktorId)
        //Fordi vi må sett en tom-dato i requesten så setter vi en dato langt frem i tid. Bør sjekkes nøyere med aap om
        // hvordan periodene man sender inn behandles (de ser ikke ut til å filtrere på periodene)
        val ettAarIFramtiden = LocalDate.now().plusYears(1).toString()

        val aapRespons = aapClient.hentAapVedtak(personIdent, oppfolgingsStartdato.toString(), ettAarIFramtiden)
        val aapIOppfolgingsPeriode = aapRespons.vedtak
            .mapNotNull { vedtak ->
                val filtrertPeriode = filtrerAapKunIOppfolgingPeriode(oppfolgingsStartdato, vedtak.periode)
                filtrertPeriode?.let { vedtak.copy(periode = it) }
            }

        val sistePeriode = aapIOppfolgingsPeriode.maxByOrNull { it.periode.fraOgMedDato }
        return sistePeriode
    }

    fun filtrerAapKunIOppfolgingPeriode(
        oppfolgingsStartdato: LocalDate,
        aapPeriode: AapVedtakResponseDto.Periode
    ): AapVedtakResponseDto.Periode? {
        if (aapPeriode.tilOgMedDato.isBefore(oppfolgingsStartdato)
        ) {
            return null
        }
        return aapPeriode
    }

    fun hentOppfolgingStartdato(aktorId: AktorId): LocalDate {
        val oppfolgingsdata = oppfolgingRepositoryV2.hentOppfolgingMedStartdato(aktorId)
            .orElseThrow { IllegalStateException("Ingen oppfølgingsdata funnet for $aktorId") }

        if (oppfolgingsdata.oppfolging && oppfolgingsdata.startDato != null) {
            return toLocalDate(oppfolgingsdata.startDato)
        }

        throw IllegalStateException("Bruker er ikke under oppfølging")
    }
}
