package no.nav.pto.veilarbportefolje.aap

import no.nav.common.types.identer.AktorId
import no.nav.pto.veilarbportefolje.aap.domene.YTELSE_KILDESYSTEM
import no.nav.pto.veilarbportefolje.aap.domene.YTELSE_TYPE
import no.nav.pto.veilarbportefolje.aap.domene.YtelserKafkaDTO
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon.Topic
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2
import no.nav.pto.veilarbportefolje.util.DateUtils.toLocalDate
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate


/**
 * Håndterer behandling av Kafka-meldinger fra [Topic.YTELSER_TOPIC].
 *
 * Denne klassen håndterer funksjonalitet knyttet til å route ytelses-kafkameldinger til riktig service basert på [YTELSE_TYPE]
 */
@Service
class YtelserKafkaService(
    private val aapService: AapService,
    private val oppfolgingRepositoryV2: OppfolgingRepositoryV2,
) : KafkaCommonNonKeyedConsumerService<YtelserKafkaDTO>() {
    private val logger: Logger = LoggerFactory.getLogger(YtelserKafkaService::class.java)

    override fun behandleKafkaMeldingLogikk(kafkaMelding: YtelserKafkaDTO) {
        if (kafkaMelding.ytelsestype != YTELSE_TYPE.AAP ) {
            logger.warn("Mottok ytelse-melding med uventet ytelsestype: ${kafkaMelding.ytelsestype}, forventet AAP. Ignorerer melding.")
            return
        }

        if (kafkaMelding.kildesystem != YTELSE_KILDESYSTEM.KELVIN) {
            logger.warn("Mottok ytelse-melding med uventet kildesystem: ${kafkaMelding.kildesystem}, forventet KELVIN. Ignorerer melding.")
            return
        }
        aapService.behandleKafkaMeldingLogikk(kafkaMelding)
    }

    fun hentOppfolgingStartdato(aktorId: AktorId): LocalDate {
        val oppfolgingsdata = oppfolgingRepositoryV2.hentOppfolgingMedStartdato(aktorId)
            .orElseThrow { IllegalStateException("Ingen oppfølgingsdata funnet") }

        if (oppfolgingsdata.oppfolging && oppfolgingsdata.startDato != null) {
            return toLocalDate(oppfolgingsdata.startDato)
        }

        secureLog.info("Fant ikke oppfolgingsdata for bruker med aktorId {}", aktorId)
        throw IllegalStateException("Bruker er ikke under oppfølging")
    }

}
