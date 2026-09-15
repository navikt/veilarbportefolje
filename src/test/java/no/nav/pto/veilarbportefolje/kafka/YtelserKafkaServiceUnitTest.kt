package no.nav.pto.veilarbportefolje.kafka

import no.nav.pto.veilarbportefolje.aap.AapService
import no.nav.pto.veilarbportefolje.dagpenger.DagpengerService
import no.nav.pto.veilarbportefolje.tiltakspenger.TiltakspengerService
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_KILDESYSTEM
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_MELDINGSTYPE
import no.nav.pto.veilarbportefolje.ytelserkafka.YTELSE_TYPE
import no.nav.pto.veilarbportefolje.ytelserkafka.YtelserKafkaDTO
import no.nav.pto.veilarbportefolje.ytelserkafka.YtelserKafkaService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class YtelserKafkaServiceUnitTest {

    private val aapService = mock(AapService::class.java)
    private val tiltakspengerService = mock(TiltakspengerService::class.java)
    private val dagpengerService = mock(DagpengerService::class.java)
    private val service = YtelserKafkaService(aapService, tiltakspengerService, dagpengerService)

    @Test
    fun `AAP-melding routes til aapService`() {
        val dto = enYtelsemelding(YTELSE_TYPE.AAP)

        service.behandleKafkaRecord(somRecord(dto))

        verify(aapService).behandleKafkaMeldingLogikk(dto)
        verifyNoInteractions(dagpengerService, tiltakspengerService)
    }

    @Test
    fun `DAGPENGER-melding routes til dagpengerService`() {
        val dto = enYtelsemelding(YTELSE_TYPE.DAGPENGER)

        service.behandleKafkaRecord(somRecord(dto))

        verify(dagpengerService).behandleKafkaMeldingLogikk(dto)
        verifyNoInteractions(aapService, tiltakspengerService)
    }

    @Test
    fun `TILTAKSPENGER-melding routes til tiltakspengerService`() {
        val dto = enYtelsemelding(YTELSE_TYPE.TILTAKSPENGER)

        service.behandleKafkaRecord(somRecord(dto))

        verify(tiltakspengerService).behandleKafkaMeldingLogikk(dto)
        verifyNoInteractions(aapService, dagpengerService)
    }

    private fun enYtelsemelding(ytelsestype: YTELSE_TYPE) = YtelserKafkaDTO(
        personId = "01010112345",
        meldingstype = YTELSE_MELDINGSTYPE.OPPRETT,
        ytelsestype = ytelsestype,
        kildesystem = YTELSE_KILDESYSTEM.KELVIN
    )

    private fun somRecord(dto: YtelserKafkaDTO) =
        ConsumerRecord("obo.ytelser-v1", 0, 0L, "key", dto)
}
