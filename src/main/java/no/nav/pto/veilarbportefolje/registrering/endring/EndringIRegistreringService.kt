package no.nav.pto.veilarbportefolje.registrering.endring

import lombok.RequiredArgsConstructor
import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.paw.besvarelse.ArbeidssokerBesvarelseEvent
import no.nav.pto.veilarbportefolje.interfaces.HandtereOppfolgingData
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2
import no.nav.pto.veilarbportefolje.util.SecureLog
import org.springframework.stereotype.Service

@RequiredArgsConstructor
@Service
@Slf4j
class EndringIRegistreringService(
    private val endringIRegistreringRepository: EndringIRegistreringRepository,
    private val opensearchIndexerV2: OpensearchIndexerV2
) : KafkaCommonConsumerService<ArbeidssokerBesvarelseEvent>(),
    HandtereOppfolgingData<AktorId> {

    public override fun behandleKafkaMeldingLogikk(kafkaMelding: ArbeidssokerBesvarelseEvent) {
        if (kafkaMelding.endret && kafkaMelding.besvarelse != null && kafkaMelding.aktorId != null) {
            val aktoerId = AktorId.of(kafkaMelding.aktorId)
            endringIRegistreringRepository.upsertEndringIRegistrering(kafkaMelding)
            opensearchIndexerV2.updateEndringerIRegistering(aktoerId, kafkaMelding)
            SecureLog.secureLog.info("Oppdatert endring i registrering for bruker: {}", aktoerId)
        }
    }

    override fun slettOppfolgingData(aktoerId: AktorId) {
        endringIRegistreringRepository.slettEndringIRegistrering(aktoerId)
        SecureLog.secureLog.info("Slettet endring i registrering for bruker: {}", aktoerId)
    }
}
