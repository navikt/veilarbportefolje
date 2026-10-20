package no.nav.pto.veilarbportefolje.sykefravaer

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.sykefravaer.dto.OppfolgingstilfelleDTO
import no.nav.pto.veilarbportefolje.sykefravaer.dto.SykefravaerPersonKafkaDTO
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

/**
 * Håndterer behandling av Kafka-meldinger fra [Topic.SYKEFRAVAEROPPFOLGING_TOPIC].
 *
 * Kafka-meldingen brukes kun som trigger for å hente oppfølgingstilfeller fra isoppfolgingstilfelle.
 * Denne klassen håndterer funksjonalitet knyttet til å lagre, oppdatere og slette
 * gjeldende oppfølgingstilfelle for en bruker som er sykmeldt nå og i oppfølgingsperioden.
 */
@Service
class SykefravaerService(
    val sykefravaerClient: SykefravaerClient,
    val sykefravaerRepository: SykefravaerRepository,
    val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    val pdlIdentRepository: PdlIdentRepository,
    val aktorClient: AktorClient,
) : KafkaCommonNonKeyedConsumerService<SykefravaerPersonKafkaDTO>() {

    public override fun behandleKafkaMeldingLogikk(kafkaMelding: SykefravaerPersonKafkaDTO) {
        secureLog.info(
            "Mottok sykefraværsmelding med uuid {} for person {}",
            kafkaMelding.uuid,
            kafkaMelding.personIdentNumber
        )

        val erUnderOppfolging = pdlIdentRepository.erBrukerUnderOppfolging(kafkaMelding.personIdentNumber)

        if (!erUnderOppfolging) {
            secureLog.info(
                "Bruker {} er ikke under oppfølging, ignorerer sykefraværsmelding.",
                kafkaMelding.personIdentNumber
            )
            return
        }

        val aktorId = aktorClient.hentAktorId(Fnr.of(kafkaMelding.personIdentNumber))
        val oppfolgingsStartdato = hentOppfolgingStartdato(aktorId)
        lagreSykefravaerForBruker(kafkaMelding.personIdentNumber, oppfolgingsStartdato)
    }

    fun hentOgLagreSykefravaerForBrukerVedOppfolgingStart(aktorId: AktorId) {
        val personIdent = aktorClient.hentFnr(aktorId).get()
        lagreSykefravaerForBruker(personIdent, LocalDate.now())
    }

    fun lagreSykefravaerForBruker(personIdent: String, oppfolgingsStartdato: LocalDate) {
        val sykefravaersPeriode = hentSykefravaersoppfolgingFraApi(personIdent, oppfolgingsStartdato)

        if (sykefravaersPeriode == null) {
            secureLog.info(
                "Bruker {} er ikke sykmeldt nå i oppfølgingsperioden, sletter eventuelle lagrede sykefravær.",
                personIdent
            )
            slettSykefravaerForAlleIdenterForBruker(personIdent)
            return
        }

        upsertSykefravaerForAktivIdentForBruker(personIdent, sykefravaersPeriode)
    }

    /**
     * Første element i oppfolgingstilfelleList er det gjeldende tilfellet dersom personen er sykmeldt nå.
     * Er end på dette tilfellet tilbake i tid, er personen ikke sykmeldt nå.
     */
    fun hentSykefravaersoppfolgingFraApi(
        personIdent: String,
        oppfolgingsStartdato: LocalDate
    ): OppfolgingstilfelleDTO? {
        val respons = sykefravaerClient.hentSykefravaersoppfolingPeriode(personIdent)
        val sistePeriode = respons.oppfolgingstilfelleList.firstOrNull() ?: return null
        val erIOppfolgingsperiode = !sistePeriode.end.isBefore(oppfolgingsStartdato)
        val erSykmeldtNaa = !sistePeriode.end.isBefore(LocalDate.now())

        return sistePeriode.takeIf { erIOppfolgingsperiode && erSykmeldtNaa }
    }

    fun upsertSykefravaerForAktivIdentForBruker(
        personIdent: String,
        sisteOppfolgingstilfelle: OppfolgingstilfelleDTO
    ) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        if (alleFnrIdenterForBruker.size > 1) {
            alleFnrIdenterForBruker.forEach { ident ->
                sykefravaerRepository.slettSykefravaerForBruker(ident)
            }
        }

        sykefravaerRepository.upsertSykefravaerOppfolgingsperiode(personIdent, sisteOppfolgingstilfelle)
    }

    fun slettSykefravaerData(aktorId: AktorId, maybeFnr: Optional<Fnr>) {
        if (maybeFnr.isEmpty) {
            secureLog.warn(
                "Kunne ikke slette sykefravær for bruker med Aktør-ID ${aktorId.get()}. Årsak fødselsnummer-parameter var tom."
            )
            return
        }

        try {
            slettSykefravaerForAlleIdenterForBruker(maybeFnr.get().toString())
        } catch (e: Exception) {
            secureLog.error("Feil ved sletting av sykefravær for bruker med fnr: ${maybeFnr.get()}", e)
            return
        }
    }

    fun slettSykefravaerForAlleIdenterForBruker(personIdent: String) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        alleFnrIdenterForBruker.forEach { ident ->
            sykefravaerRepository.slettSykefravaerForBruker(ident)
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
