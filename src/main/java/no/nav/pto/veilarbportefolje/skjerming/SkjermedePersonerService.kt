package no.nav.pto.veilarbportefolje.skjerming

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.utils.EnvironmentUtils
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonKeyedConsumerService
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2
import no.nav.pto.veilarbportefolje.util.DateUtils
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SkjermedePersonerService(
    private val skjermingRepository: SkjermingRepository,
    private val brukerService: BrukerServiceV2,
    private val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt
) : KafkaCommonKeyedConsumerService<SkjermingDTO?>() {

    override fun behandleKafkaRecordLogikk(
        kafkaRecordValue: SkjermingDTO?,
        kafkaKey: String
    ) {
        val fnr = Fnr.of(kafkaKey)

        val isDev: Boolean = EnvironmentUtils.isDevelopment().orElse(false) ?: false
        if (isDev && kafkaRecordValue == null) {
            secureLog.info(
                String.format(
                    "Ignorerer dårlig datakvalitet i dev, bruker fnr %s, kafka melding: %s",
                    fnr.get(),
                    kafkaRecordValue
                )
            )
            return
        }

        val skjermetFra: LocalDateTime? =
            if (kafkaRecordValue?.skjermetFra != null && kafkaRecordValue.skjermetFra.size >= 5) {
                LocalDateTime.of(
                    kafkaRecordValue.skjermetFra[0],
                    kafkaRecordValue.skjermetFra[1],
                    kafkaRecordValue.skjermetFra[2],
                    kafkaRecordValue.skjermetFra[3],
                    kafkaRecordValue.skjermetFra[4],
                    0
                )
            } else {
                null
            }

        val skjermetTil: LocalDateTime? =
            if (kafkaRecordValue?.skjermetTil != null && kafkaRecordValue.skjermetTil.size >= 5) {
                kafkaRecordValue.skjermetTil.let {
                    LocalDateTime.of(
                        it[0],
                        it[1],
                        it[2],
                        it[3],
                        it[4],
                        0
                    )
                }
            } else {
                null
            }

        if (skjermetFra == null && skjermetTil == null) {
            throw Exception("Possible illegal data about skjerming period, kafka message: $kafkaRecordValue")
        }

        skjermingRepository.settSkjermingPeriode(
            fnr,
            DateUtils.toTimestamp(skjermetFra),
            DateUtils.toTimestamp(skjermetTil)
        )

        brukerService.hentAktorId(fnr).ifPresent { aktorId: AktorId ->
            opensearchIndexerPaDatafelt.updateSkjermetTil(
                aktorId,
                skjermetTil
            )
        }
    }
}

@Service
class SkjermingStatusService(
    private val skjermingRepository: SkjermingRepository,
    private val brukerService: BrukerServiceV2,
    private val opensearchIndexerPaDatafelt: OpensearchIndexerPaDatafelt
) : KafkaCommonKeyedConsumerService<String?>() {

    override fun behandleKafkaRecordLogikk(kafkaRecordValue: String?, kafkaKey: String) {
        val fnr = Fnr.of(kafkaKey)
        val erSkjermet = kafkaRecordValue != null && kafkaRecordValue.toBoolean()

        if (erSkjermet) {
            skjermingRepository.settSkjerming(fnr, true)
        } else {
            skjermingRepository.deleteSkjermingData(fnr)
        }

        brukerService.hentAktorId(fnr).ifPresent { aktorId: AktorId ->
            opensearchIndexerPaDatafelt.updateErSkjermet(
                aktorId,
                erSkjermet
            )
        }
    }
}
