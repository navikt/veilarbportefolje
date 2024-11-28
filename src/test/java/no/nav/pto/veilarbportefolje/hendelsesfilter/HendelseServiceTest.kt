package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.common.types.identer.Fnr
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNorskIdent
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.net.URI
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

class HendelseServiceTest(
    @Autowired private val hendelseService: HendelseService
) : EndToEndTest() {

    @Test
    fun `skal starte hendelse`() {
        val norskIdent = randomNorskIdent()
        val fnr = Fnr.of(norskIdent.get())
        val aktorId = randomAktorId()
        insertOppfolgingsInformasjon(aktorId, fnr)
        val key = "96463d56-019e-4b30-ae9b-7365cf002a09"
        val hendelseRecordValue = HendelseRecordValue(
            personID = norskIdent,
            avsender = "dev-gcp:dab:aktivitetsplan",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValue.HendelseInnhold(
                navn = "Bruker har et utgått varsel",
                dato = ZonedDateTime.of(2024, 11, 27, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                lenke = URI.create("https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan").toURL(),
                detaljer = null,
            ),
        )
        val hendelseRecord =
            ConsumerRecord(
                KafkaConfigCommon.Topic.PORTEFOLJE_HENDELSESFILTER.topicName,
                0,
                0,
                key,
                hendelseRecordValue
            )

        hendelseService.behandleKafkaRecord(hendelseRecord)
        val lagretHendelse = hendelseService.hentHendelse(UUID.fromString(key))

        assertThat(lagretHendelse).isNotNull
    }
}