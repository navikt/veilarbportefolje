package no.nav.pto.veilarbportefolje.sykefravaer

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.client.AktorClient
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.sykefravaer.dto.SykefravaerPersonKafkaDTO
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SykefravaerService(
    private val sykefravaerClient: SykefravaerClient,
    val pdlIdentRepository: PdlIdentRepository,
    val aktorClient: AktorClient,
) : KafkaCommonNonKeyedConsumerService<SykefravaerPersonKafkaDTO>() {
    private val logger = LoggerFactory.getLogger(SykefravaerService::class.java)

    public override fun behandleKafkaMeldingLogikk(kafkaMelding: SykefravaerPersonKafkaDTO) {
        // log received message in securelogs
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
        lagreSykefravaerForBruker(kafkaMelding.personIdentNumber, aktorId)
    }


    fun lagreSykefravaerForBruker(personIdent: String, aktorId: AktorId) {
        val oppfolgingstilfellePerson = sykefravaerClient.hentOppfolgingstilfelle(personIdent)
        logger.info(
            "Hentet ${oppfolgingstilfellePerson.oppfolgingstilfelleList.size} oppfølgingstilfeller for person $personIdent"
        )
    }
}
