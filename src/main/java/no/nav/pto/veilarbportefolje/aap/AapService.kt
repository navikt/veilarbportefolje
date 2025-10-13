package no.nav.pto.veilarbportefolje.aap

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.aap.domene.*
import no.nav.pto.veilarbportefolje.aap.repository.AapRepository
import no.nav.pto.veilarbportefolje.domene.AktorClient
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDate
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_KILDESYSTEM
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_MELDINGSTYPE
import no.nav.pto.veilarbportefolje.ytelserkafka.YtelserKafkaDTO
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*


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
    val aapRepository: AapRepository,
    val opensearchIndexerV2: OpensearchIndexerV2
) {
    private val logger: Logger = LoggerFactory.getLogger(AapService::class.java)

    @Transactional
    fun behandleKafkaMeldingLogikk(kafkaMelding: YtelserKafkaDTO) {
        if (kafkaMelding.kildesystem != YTELSE_KILDESYSTEM.KELVIN) {
            logger.warn("Mottok ytelse-melding  for AAP med uventet kildesystem: ${kafkaMelding.kildesystem}, forventet KELVIN. Ignorerer melding.")
            return
        }
        val erUnderOppfolging = pdlIdentRepository.erBrukerUnderOppfolging(kafkaMelding.personId)

        if (!erUnderOppfolging) {
            secureLog.info("Bruker {} er ikke under oppfølging, ignorerer aap-ytelse melding.", kafkaMelding.personId)
            return
        }

        val aktorId = aktorClient.hentAktorId(Fnr.of(kafkaMelding.personId))
        val oppfolgingsStartdato = hentOppfolgingStartdato(aktorId)
        lagreAapForBruker(kafkaMelding.personId, aktorId, oppfolgingsStartdato, kafkaMelding.meldingstype)
    }

    fun hentOgLagreAapForBrukerVedBatchjobb(aktorId: AktorId) {
        val personIdent = aktorClient.hentFnr(aktorId).get()
        val oppfolgingsStartdato = hentOppfolgingStartdato(aktorId)
        lagreAapForBruker(personIdent, aktorId, oppfolgingsStartdato, YTELSE_MELDINGSTYPE.OPPRETT)
    }

    fun hentOgLagreAapForBrukerVedOppfolgingStart(aktorId: AktorId) {
        val personIdent = aktorClient.hentFnr(aktorId).get()
        lagreAapForBruker(personIdent, aktorId, LocalDate.now(), YTELSE_MELDINGSTYPE.OPPRETT)
    }

    fun lagreAapForBruker(personIdent: String, aktorId: AktorId, oppfolgingsStartdato: LocalDate, meldingstype: YTELSE_MELDINGSTYPE) {
        val sisteAapPeriode = hentSisteAapPeriodeFraApi(personIdent, oppfolgingsStartdato)

        if (sisteAapPeriode == null)
            if (meldingstype == YTELSE_MELDINGSTYPE.OPPDATER) {
                secureLog.info("Ingen AAP-periode funnet i oppfølgingsperioden for bruker {}, " +
                        "sletter eventuell eksisterende AAP-periode i databasen", personIdent)
                slettAapForAlleIdenterForBruker(personIdent)
                opensearchIndexerV2.slettAapKelvin(aktorId)
                return
            } else {
                secureLog.info("Ingen AAP-periode funnet i oppfølgingsperioden for bruker {}, ignorerer aap-ytelse melding.", personIdent)
                return
            }

        val harAktivAap = sisteAapPeriode.status == VedtakStatus.LØPENDE && sisteAapPeriode.periode.tilOgMedDato.isAfter(
            LocalDate.now().minusDays(1)
        )

        upsertAapForAktivIdentForBruker(personIdent, aktorId, sisteAapPeriode)
        opensearchIndexerV2.oppdaterAapKelvin(
            aktorId,
            harAktivAap,
            sisteAapPeriode.periode.tilOgMedDato,
            sisteAapPeriode.rettighetsType
        )
    }

    fun upsertAapForAktivIdentForBruker(personIdent: String, aktorId: AktorId, sisteAapPeriode: AapVedtakResponseDto.Vedtak) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        if (alleFnrIdenterForBruker.size > 1) {
            alleFnrIdenterForBruker.forEach { ident ->
                aapRepository.slettAapForBruker(ident)
            }
        }

        aapRepository.upsertAap(personIdent, sisteAapPeriode)
    }

    fun hentSisteAapPeriodeFraApi(personIdent: String, oppfolgingsStartdato: LocalDate): AapVedtakResponseDto.Vedtak? {
        //Fordi vi må sett en tom-dato i requesten så setter vi en dato langt frem i tid.
        val ettAarIFramtiden = LocalDate.now().plusYears(1).toString()

        val aapRespons = aapClient.hentAapVedtak(personIdent, oppfolgingsStartdato.toString(), ettAarIFramtiden)
        val aapIOppfolgingsPeriode = aapRespons.vedtak
            .filter { vedtak ->
                vedtak.periode.tilOgMedDato.isAfter(oppfolgingsStartdato.minusDays(1))
            }

        val sistePeriode = aapIOppfolgingsPeriode.maxByOrNull { it.periode.fraOgMedDato }
        return sistePeriode
    }

    fun slettAapData(aktorId: AktorId, maybeFnr: Optional<Fnr>) {
        if (maybeFnr.isEmpty) {
            secureLog.warn(
                "Kunne ikke slette AAP bruker med Aktør-ID {}. Årsak fødselsnummer-parameter var tom.",
                aktorId.get()
            )
            return
        }

        try {
            slettAapForAlleIdenterForBruker(maybeFnr.get().toString())
        } catch (e: Exception) {
            secureLog.error("Feil ved sletting av AAP data for bruker med fnr: ${maybeFnr.get()}", e)
            return
        }
    }

    fun slettAapForAlleIdenterForBruker(personIdent: String) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        alleFnrIdenterForBruker.forEach { ident ->
            aapRepository.slettAapForBruker(ident)
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
