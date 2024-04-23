package no.nav.pto.veilarbportefolje.arbeidssoker.registrering.endring

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2
import no.nav.pto.veilarbportefolje.util.SecureLog
import org.springframework.stereotype.Service

@RequiredArgsConstructor
@Service
@Slf4j
class EndringIArbeidssokerRegistreringService(private val endringIArbeidssokerRegistreringRepository: EndringIArbeidssokerRegistreringRepository, private val opensearchIndexerV2: OpensearchIndexerV2) : KafkaCommonConsumerService<ArbeidssokerBesvarelseEvent>() {

    public override fun behandleKafkaMeldingLogikk(kafkaMelding: ArbeidssokerBesvarelseEvent) {
        if (kafkaMelding.endret && kafkaMelding.besvarelse != null && kafkaMelding.aktorId != null) {
            val aktoerId = AktorId.of(kafkaMelding.aktorId)
            endringIArbeidssokerRegistreringRepository.upsertEndringIRegistrering(kafkaMelding)
            opensearchIndexerV2.updateEndringerIRegistering(aktoerId, kafkaMelding)
            SecureLog.secureLog.info("Oppdatert endring i registrering for bruker: {}", aktoerId)
        }
    }

    fun slettEndringIRegistering(aktoerId: AktorId) {
        endringIArbeidssokerRegistreringRepository.slettEndringIRegistrering(aktoerId)
        SecureLog.secureLog.info("Slettet endring i registrering for bruker: {}", aktoerId)
    }
}
