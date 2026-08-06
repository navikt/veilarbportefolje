package no.nav.pto.veilarbportefolje.skjerming

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.sql.Timestamp
import java.util.*

class SkjermedePersonerServiceTest {
    private lateinit var skjermedePersonerService: SkjermedePersonerService
    private lateinit var skjermingStatusService: SkjermingStatusService
    private lateinit var skjermingRepository: SkjermingRepository

    @BeforeEach
    fun setUp() {
        skjermingRepository = mock(SkjermingRepository::class.java)
        val brukerServiceV2 = mock(BrukerServiceV2::class.java)
        `when`(brukerServiceV2.hentAktorId(any()))
            .thenReturn(Optional.of(AktorId.of("1111")))
        val opensearchIndexerPaDatafelt =
            mock(OpensearchIndexerPaDatafelt::class.java)
        skjermedePersonerService =
            SkjermedePersonerService(skjermingRepository, brukerServiceV2, opensearchIndexerPaDatafelt)
        skjermingStatusService =
            SkjermingStatusService(skjermingRepository, brukerServiceV2, opensearchIndexerPaDatafelt)
    }

    @Test
    fun testSavingSkjermingStatus() {
        val fnr = Fnr.of("fnr123")
        var consumerRecord = ConsumerRecord("topic", 1, 2, fnr.get(), "true")
        skjermingStatusService.behandleKafkaRecord(consumerRecord)

        verify(skjermingRepository, times(1)).settSkjerming(fnr, true)

        consumerRecord = ConsumerRecord("topic", 1, 2, fnr.get(), "false")
        skjermingStatusService.behandleKafkaRecord(consumerRecord)

        verify(skjermingRepository, times(1)).deleteSkjermingData(fnr)
    }

    @Test
    fun testSavingSkjermingPersoner() {
        val fnr = Fnr.of("fnr123")

        var consumerRecord = ConsumerRecord(
            "topic",
            1,
            2,
            fnr.get(),
            SkjermingDTO(
                intArrayOf(2022, 2, 22, 13, 14, 0), null
            )
        )
        skjermedePersonerService.behandleKafkaRecord(consumerRecord)

        verify(skjermingRepository, times(1))
            .settSkjermingPeriode(fnr, Timestamp.valueOf("2022-02-22 13:14:00"), null)

        consumerRecord = ConsumerRecord(
            "topic",
            1,
            2,
            fnr.get(),
            SkjermingDTO(intArrayOf(2022, 2, 22, 13, 14, 0), intArrayOf(2022, 4, 22, 13, 14, 0))
        )
        skjermedePersonerService.behandleKafkaRecord(consumerRecord)

        verify(skjermingRepository, times(1)).settSkjermingPeriode(
            fnr,
            Timestamp.valueOf("2022-02-22 13:14:00"),
            Timestamp.valueOf("2022-04-22 13:14:00")
        )
    }
}
