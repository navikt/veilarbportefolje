package no.nav.pto.veilarbportefolje.ungdomsprogram

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDateOrNull
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import java.time.LocalDate


/**
 * MERK: Når vi får kafkameldinger skal denne ytelsen kobles på å håndterere behandling av Kafka-meldinger fra [Topic.YTELSER_TOPIC].
 * Disse blir routet fra YtelserKafkaService. I mellomtiden settes det opp en chron-jobb som heter ut dataene.
 *
 * Denne klassen håndterer funksjonalitet knyttet til å starte (les: lagre), oppdatere og stoppe (les: slette)
 * ytelser for ungdomsprogramytelsen.
 */
@Service
class UngdomsprogramService(
    val ungdomsprogramClient: UngdomsprogramClient,
    val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    val pdlIdentRepository: PdlIdentRepository,
    val aktorClient: AktorClient,
) {

    fun hentUngdomsprogramForAlleBrukere() {
        val alleBrukere = ungdomsprogramClient.hentAlleMedUngdomsprogram().deltakelser

        for (bruker in alleBrukere) {
            try {
                val erUnderOppfolging = pdlIdentRepository.erBrukerUnderOppfolging(bruker.deltakerIdent)
                if (!erUnderOppfolging) {
                    secureLog.info(
                        "Bruker {} er ikke under oppfølging, ignorerer ungdomsprogram-data på bruker",
                        bruker.deltakerIdent
                    )
                    continue
                }
                val aktorId = aktorClient.hentAktorId(Fnr.of(bruker.deltakerIdent))
                val oppfolgingsStartdato = hentOppfolgingStartdato(aktorId)

                val tilOgMed = bruker.periode.tilOgMed
                val ytelsenErIOppfolgingsPeriode =
                    tilOgMed == null || tilOgMed.isAfter(oppfolgingsStartdato.minusDays(1))

                if (!ytelsenErIOppfolgingsPeriode) {
                    secureLog.info(
                        "Ingen ungdomsprogramperiode funnet i oppfølgingsperioden for bruker {}, " +
                                "sletter eventuell eksisterende ytelse-periode i databasen",
                        bruker.deltakerIdent
                    )
                    //todo: slette alle identer i ungdomsprogram i db, og i opensearch
                    continue
                }

                val ytelsesperiodenErAktiv = tilOgMed == null || tilOgMed.isAfter(LocalDate.now().minusDays(1))
                // todo: upsert perioden i db, og oppdater opensearch

            } catch (e: Exception) {
                secureLog.error("Feil ved behandling av ungdomsprogram for ${bruker.deltakerIdent}", e)
            }
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
