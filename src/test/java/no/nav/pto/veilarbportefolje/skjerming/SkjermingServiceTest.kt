package no.nav.pto.veilarbportefolje.skjerming

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.sql.Timestamp
import java.util.*

class SkjermingServiceTest {
    private lateinit var skjermingService: SkjermingService
    private lateinit var skjermingRepository: SkjermingRepository

    @BeforeEach
    fun setUp() {
        skjermingRepository = Mockito.mock(SkjermingRepository::class.java)
        val brukerServiceV2 = Mockito.mock(BrukerServiceV2::class.java)
        `when`(brukerServiceV2.hentAktorId(Mockito.any<Fnr?>()))
            .thenReturn(Optional.of(AktorId.of("1111")))
        val opensearchIndexerPaDatafelt =
            Mockito.mock(OpensearchIndexerPaDatafelt::class.java)
        skjermingService = SkjermingService(skjermingRepository, brukerServiceV2, opensearchIndexerPaDatafelt)
    }

    @Test
    fun testSavingSkjermingStatus() {
        val fnr = Fnr.of("fnr123")
        var consumerRecord = ConsumerRecord("topic", 1, 2, fnr.get(), "true")
        skjermingService.behandleSkjermingStatus(consumerRecord)

        Mockito.verify(skjermingRepository, Mockito.times(1)).settSkjerming(fnr, true)

        consumerRecord = ConsumerRecord("topic", 1, 2, fnr.get(), "false")
        skjermingService.behandleSkjermingStatus(consumerRecord)

        Mockito.verify(skjermingRepository, Mockito.times(1)).deleteSkjermingData(fnr)
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
        skjermingService.behandleSkjermedePersoner(consumerRecord)

        Mockito.verify(skjermingRepository, Mockito.times(1))
            .settSkjermingPeriode(fnr, Timestamp.valueOf("2022-02-22 13:14:00"), null)

        consumerRecord = ConsumerRecord(
            "topic",
            1,
            2,
            fnr.get(),
            SkjermingDTO(intArrayOf(2022, 2, 22, 13, 14, 0), intArrayOf(2022, 4, 22, 13, 14, 0))
        )
        skjermingService.behandleSkjermedePersoner(consumerRecord)

        Mockito.verify(skjermingRepository, Mockito.times(1)).settSkjermingPeriode(
            fnr,
            Timestamp.valueOf("2022-02-22 13:14:00"),
            Timestamp.valueOf("2022-04-22 13:14:00")
        )
    }
}
