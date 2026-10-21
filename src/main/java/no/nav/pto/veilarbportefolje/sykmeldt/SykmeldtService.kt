package no.nav.pto.veilarbportefolje.sykmeldt

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.sykmeldt.dto.OppfolgingstilfelleDTO
import no.nav.pto.veilarbportefolje.sykmeldt.dto.SykmeldtPersonKafkaDTO
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

/**
 * Håndterer behandling av Kafka-meldinger fra [Topic.SYKMELDT_TOPIC].
 *
 * Kafka-meldingen brukes kun som trigger for å hente sykmeldtperioder fra isoppfolgingstilfelle.
 * Dette er brukere som er sykmeldte og under oppfølging, både med og uten arbeidsgiver.
 * Denne klassen håndterer funksjonalitet knyttet til å lagre, oppdatere og slette
 * gjeldende sykmeldtperiode for en bruker som er sykmeldt i oppfølgingsperioden.
 */
@Service
class SykmeldtService(
    val sykmeldtClient: SykmeldtClient,
    val sykmeldtRepository: SykmeldtRepository,
    val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    val pdlIdentRepository: PdlIdentRepository,
    val aktorClient: AktorClient,
) : KafkaCommonNonKeyedConsumerService<SykmeldtPersonKafkaDTO>() {

    public override fun behandleKafkaMeldingLogikk(kafkaMelding: SykmeldtPersonKafkaDTO) {
        secureLog.info(
            "Mottok sykmeldtmelding med uuid {} for person {}",
            kafkaMelding.uuid,
            kafkaMelding.personIdentNumber
        )

        val erUnderOppfolging = pdlIdentRepository.erBrukerUnderOppfolging(kafkaMelding.personIdentNumber)

        if (!erUnderOppfolging) {
            secureLog.info(
                "Bruker {} er ikke under oppfølging, ignorerer sykmeldtmelding.",
                kafkaMelding.personIdentNumber
            )
            return
        }

        val aktorId = aktorClient.hentAktorId(Fnr.of(kafkaMelding.personIdentNumber))
        val oppfolgingsStartdato = hentOppfolgingStartdato(aktorId)
        lagreSykmeldtperiodeForBruker(kafkaMelding.personIdentNumber, oppfolgingsStartdato)
    }

    fun hentOgLagreSykmeldtperiodeForBrukerVedOppfolgingStart(aktorId: AktorId) {
        val personIdent = aktorClient.hentFnr(aktorId).get()
        lagreSykmeldtperiodeForBruker(personIdent, LocalDate.now())
    }

    fun hentOgLagreSykmeldtperiodeVedAdminjobb(aktorId: AktorId) {
        val personIdent = aktorClient.hentFnr(aktorId)
        if (personIdent == null) {
            secureLog.warn("Batchjobb for sykmeldte - kunne ikke hente fødselsnummer for aktørId $aktorId")
            return
        }
        val oppfolgingsStartdato = hentOppfolgingStartdato(aktorId)
        lagreSykmeldtperiodeForBruker(personIdent.get(), oppfolgingsStartdato)
    }

    fun lagreSykmeldtperiodeForBruker(personIdent: String, oppfolgingsStartdato: LocalDate) {
        val sykmeldtPeriode = hentSykmeldtperiodeFraApi(personIdent, oppfolgingsStartdato)

        if (sykmeldtPeriode == null) {
            secureLog.info(
                "Bruker {} er ikke sykmeldt nå i oppfølgingsperioden, sletter eventuelle lagrede sykmeldtperioder.",
                personIdent
            )
            slettSykmeldtForAlleIdenterForBruker(personIdent)
            return
        }

        upsertSykmeldtperiodeForAktivIdentForBruker(personIdent, sykmeldtPeriode)
    }

    /**
     * Første element i oppfolgingstilfelleList er det gjeldende tilfellet dersom personen er sykmeldt nå.
     * Er end på dette tilfellet tilbake i tid, er personen ikke lengre sykmeldt.
     */
    fun hentSykmeldtperiodeFraApi(
        personIdent: String,
        oppfolgingsStartdato: LocalDate
    ): OppfolgingstilfelleDTO? {
        val respons = sykmeldtClient.hentSykmeldtPeriode(personIdent)
        val sistePeriode = respons.oppfolgingstilfelleList.firstOrNull() ?: return null
        val erIOppfolgingsperiode = !sistePeriode.end.isBefore(oppfolgingsStartdato)
        val erSykmeldtNaa = !sistePeriode.end.isBefore(LocalDate.now())

        return sistePeriode.takeIf { erIOppfolgingsperiode && erSykmeldtNaa }
    }

    fun upsertSykmeldtperiodeForAktivIdentForBruker(
        personIdent: String,
        sisteOppfolgingstilfelle: OppfolgingstilfelleDTO
    ) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        if (alleFnrIdenterForBruker.size > 1) {
            alleFnrIdenterForBruker.forEach { ident ->
                sykmeldtRepository.slettSykmeldtForBruker(ident)
            }
        }

        sykmeldtRepository.upsertSykmeldtperiode(personIdent, sisteOppfolgingstilfelle)
    }

    fun slettSykmeldtData(aktorId: AktorId, maybeFnr: Optional<Fnr>) {
        if (maybeFnr.isEmpty) {
            secureLog.warn(
                "Kunne ikke slette sykmeldtdata for bruker med Aktør-ID ${aktorId.get()}. Årsak fødselsnummer-parameter var tom."
            )
            return
        }

        try {
            slettSykmeldtForAlleIdenterForBruker(maybeFnr.get().toString())
        } catch (e: Exception) {
            secureLog.error("Feil ved sletting av sykmeldtdata for bruker med fnr: ${maybeFnr.get()}", e)
            return
        }
    }

    fun slettSykmeldtForAlleIdenterForBruker(personIdent: String) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        alleFnrIdenterForBruker.forEach { ident ->
            sykmeldtRepository.slettSykmeldtForBruker(ident)
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
