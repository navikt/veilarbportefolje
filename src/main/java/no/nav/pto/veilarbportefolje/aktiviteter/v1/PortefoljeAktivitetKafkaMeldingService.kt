package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.pto.veilarbportefolje.kafka.tilKafkaMeldingMedMetadata
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Service

@Service
class PortefoljeAktivitetKafkaMeldingService(
    private val repository: PortefoljeAktivitetKafkaMeldingRepository,
) {
    fun behandleKafkaRecords(kafkaRecords: List<ConsumerRecord<String, PortefoljeAktivitetKafkaMelding>>): PortefoljeAktivitetBatchResult {
        if (kafkaRecords.isEmpty()) {
            return PortefoljeAktivitetBatchResult(0, 0, 0, 0)
        }

        secureLog.info(
            "Behandler kafka-batch med {} meldinger fra topic {} partition {} (offset {}-{}).",
            kafkaRecords.size,
            kafkaRecords.first().topic(),
            kafkaRecords.first().partition(),
            kafkaRecords.first().offset(),
            kafkaRecords.last().offset(),
        )

        val resultat = repository.behandleAktivitetsKafkaMeldinger(
            kafkaRecords.map { it.tilKafkaMeldingMedMetadata().toEntity() },
        )

        if (resultat.ignorert > 0) {
            secureLog.info(
                "{} meldinger ble ikke prosessert fordi nyere versjon allerede er lagret eller fordi de ble overstyrt av en nyere melding i samme batch.",
                resultat.ignorert,
            )
        }

        return resultat
    }
}
