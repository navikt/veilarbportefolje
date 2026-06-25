package no.nav.pto.veilarbportefolje.aktiviteter.v1

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`

class PortefoljeAktivitetKafkaMeldingServiceTest {
    private val repository = mock(PortefoljeAktivitetKafkaMeldingRepository::class.java)
    private val service = PortefoljeAktivitetKafkaMeldingService(repository)

    @Test
    fun `skal mappe ConsumerRecords til entities og delegere til repository`() {
        val melding = lagMelding()
        val record = ConsumerRecord("pto.aktivitet-portefolje-v1", 3, 42L, "record-key", melding)
        val expectedEntity = melding.toEntity(
            KafkaMeldingMetadata(recordOffset = 42L, recordPartition = 3, recordKey = "record-key"),
        )

        `when`(repository.behandleAktivitetsKafkaMeldinger(listOf(expectedEntity))).thenReturn(
            PortefoljeAktivitetBatchResult(mottatte = 1, dedupliserte = 0, prosesserte = 1, ignorert = 0),
        )

        service.behandleKafkaRecords(listOf(record))

        verify(repository).behandleAktivitetsKafkaMeldinger(listOf(expectedEntity))
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun `skal returnere tomt resultat for tom liste`() {
        val resultat = service.behandleKafkaRecords(emptyList())

        assert(resultat.mottatte == 0)
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun `skal sende flere records som en batch til repository`() {
        val melding1 = lagMelding(aktivitetId = "a1")
        val melding2 = lagMelding(aktivitetId = "a2")
        val record1 = ConsumerRecord("pto.aktivitet-portefolje-v1", 0, 10L, "key-1", melding1)
        val record2 = ConsumerRecord("pto.aktivitet-portefolje-v1", 0, 11L, "key-2", melding2)

        val expectedEntities = listOf(
            melding1.toEntity(KafkaMeldingMetadata(recordOffset = 10L, recordPartition = 0, recordKey = "key-1")),
            melding2.toEntity(KafkaMeldingMetadata(recordOffset = 11L, recordPartition = 0, recordKey = "key-2")),
        )

        `when`(repository.behandleAktivitetsKafkaMeldinger(expectedEntities)).thenReturn(
            PortefoljeAktivitetBatchResult(mottatte = 2, dedupliserte = 0, prosesserte = 2, ignorert = 0),
        )

        service.behandleKafkaRecords(listOf(record1, record2))

        verify(repository).behandleAktivitetsKafkaMeldinger(expectedEntities)
    }

    private fun lagMelding(aktivitetId: String = "aktivitet-1") = PortefoljeAktivitetKafkaMelding(
        aktivitetId = aktivitetId,
        version = 2,
        aktorId = "aktor-1",
        fraDato = "2024-01-01T10:15:30Z",
        tilDato = "2024-02-01T10:15:30Z",
        endretDato = "2024-03-01T10:15:30Z",
        aktivitetType = "STILLING_FRA_NAV",
        aktivitetStatus = "GJENNOMFORES",
        endringsType = "REDIGERT",
        lagtInnAv = "NAV",
        stillingFraNavData = PortefoljeAktivitetKafkaMelding.StillingFraNavPortefoljeData(
            cvKanDelesStatus = "IKKE_SVART",
            svarfrist = "2024-04-01",
        ),
        avtalt = true,
        historisk = false,
        tiltakskode = "ARBFORB",
    )
}
