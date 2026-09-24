package no.nav.pto.veilarbportefolje.uforetrygd

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.uforetrygd.dto.UforetrygdResponseDto
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_KILDESYSTEM
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_MELDINGSTYPE
import no.nav.pto.veilarbportefolje.ytelserkafka.YtelserKafkaDTO
import org.springframework.stereotype.Service
import java.util.*


/**
 * Håndterer behandling av Kafka-meldinger fra [Topic.YTELSER_TOPIC] av typen [YTELSE_TYPE.UFORETRYGD].
 * Disse blir routet fra YtelserKafkaService
 *
 * Denne klassen håndterer funksjonalitet knyttet til å starte (les: lagre), oppdatere og stoppe (les: slette)
 * ytelser for uføretrygd.
 */
@Service
class UforetrygdService(
    val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
    val pdlIdentRepository: PdlIdentRepository,
    val aktorClient: AktorClient,
    val uforetrygdRepository: UforetrygdRepository,
    val uforetrygdClient: UforetrygdClient
) {
    private val logger = org.slf4j.LoggerFactory.getLogger(UforetrygdService::class.java)

    fun behandleKafkaMeldingLogikk(kafkaMelding: YtelserKafkaDTO) {
        if (kafkaMelding.kildesystem != YTELSE_KILDESYSTEM.XXX) {
            logger.warn("Mottok ytelse-melding for Tiltakspenger med uventet kildesystem : ${kafkaMelding.kildesystem}, forventet XXX. Ignorerer melding.")
            return
        }

        val aktorId = aktorClient.hentAktorId(Fnr.of(kafkaMelding.personId))

        if (kafkaMelding.meldingstype == YTELSE_MELDINGSTYPE.SLETT) {
            slettUforetrygdData(aktorId, Optional.of(Fnr.of(kafkaMelding.personId)))
            return
        }

        val erUnderOppfolging = pdlIdentRepository.erBrukerUnderOppfolging(kafkaMelding.personId)

        if (!erUnderOppfolging) {
            secureLog.info(
                "Bruker {} er ikke under oppfølging, ignorerer uføretrygd-ytelse melding.",
                kafkaMelding.personId
            )
            return
        }

        lagreUforetrygdForBruker(kafkaMelding.personId)
    }


    fun lagreUforetrygdForBruker(
        personIdent: String
    ) {
        val uføretrygd = uforetrygdClient.hentUforetrygd(personIdent)

        if (uføretrygd == null) {
            secureLog.info(
                "Ingen uføretrygd funnet for bruker {}, ignorerer uføretrygd-ytelse melding.",
                personIdent
            )
            return
        }

        upsertUforetrygdForAktivIdentForBruker(personIdent, uføretrygd)
    }

    fun upsertUforetrygdForAktivIdentForBruker(
        personIdent: String,
        uføretrygd: UforetrygdResponseDto
    ) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        if (alleFnrIdenterForBruker.size > 1) {
            alleFnrIdenterForBruker.forEach { ident ->
                uforetrygdRepository.slettUforetrygdForBruker(ident)
            }
        }

        uforetrygdRepository.upsertUforetrygd(personIdent, uføretrygd)
    }

    fun slettUforetrygdData(aktorId: AktorId, maybeFnr: Optional<Fnr>) {
        if (maybeFnr.isEmpty) {
            secureLog.warn(
                "Kunne ikke slette uføretrygd bruker med Aktør-ID ${aktorId.get()}. Årsak fødselsnummer-parameter var tom."
            )
            return
        }

        try {
            slettUforetrygdForAlleIdenterForBruker(maybeFnr.get().toString())
        } catch (e: Exception) {
            secureLog.error("Feil ved sletting av uføretrygd data for bruker med fnr: ${maybeFnr.get()}", e)
            return
        }
    }

    fun slettUforetrygdForAlleIdenterForBruker(personIdent: String) {
        val alleFnrIdenterForBruker = pdlIdentRepository.hentFnrIdenterForBruker(personIdent).identer
        alleFnrIdenterForBruker.forEach { ident ->
            uforetrygdRepository.slettUforetrygdForBruker(ident)
        }
    }

}
