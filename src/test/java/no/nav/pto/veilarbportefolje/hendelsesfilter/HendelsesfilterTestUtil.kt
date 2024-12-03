package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.common.types.identer.NorskIdent
import no.nav.pto.veilarbportefolje.kafka.KafkaConfigCommon
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNorskIdent
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomZonedDate
import org.apache.kafka.clients.consumer.ConsumerRecord
import java.net.URI
import java.net.URL
import java.time.ZonedDateTime
import java.util.*
import kotlin.random.Random

// For å kunne kalle funksjonen fra Java uten å måtte sende inn argument
fun genererRandomHendelse(): Hendelse {
    return genererRandomHendelse(
        UUID.randomUUID(),
        randomNorskIdent(),
        randomAvsender(),
        randomKategori(),
        randomBeskrivelse(),
        randomZonedDate(),
        randomUrl(),
        randomDetaljer(),
    )
}

fun genererRandomHendelse(
    id: UUID = UUID.randomUUID(),
    personIdent: NorskIdent = randomNorskIdent(),
    avsender: String = randomAvsender(),
    kategori: Kategori = randomKategori(),
    hendelseBeskrivelse: String = randomBeskrivelse(),
    hendelseDato: ZonedDateTime = randomZonedDate(),
    hendelseLenke: URL = randomUrl(),
    hendelseDetaljer: String? = randomDetaljer(),
): Hendelse {
    return Hendelse(
        id = id,
        personIdent = personIdent,
        avsender = avsender,
        kategori = kategori,
        hendelse = Hendelse.HendelseInnhold(
            beskrivelse = hendelseBeskrivelse,
            dato = hendelseDato,
            lenke = hendelseLenke,
            detaljer = hendelseDetaljer
        )
    )
}

fun genererRandomHendelseRecordValue(
    personID: NorskIdent = randomNorskIdent(),
    avsender: String = randomAvsender(),
    kategori: Kategori = randomKategori(),
    operasjon: Operasjon = randomOperasjon(),
    hendelseBeskrivelse: String = randomBeskrivelse(),
    hendelseDato: ZonedDateTime = randomZonedDate(),
    hendelseLenke: URL = randomUrl(),
    hendelseDetaljer: String? = randomDetaljer(),
): HendelseRecordValue {
    return HendelseRecordValue(
        personID = personID,
        avsender = avsender,
        kategori = kategori,
        operasjon = operasjon,
        hendelse = HendelseRecordValue.HendelseInnhold(
            beskrivelse = hendelseBeskrivelse,
            dato = hendelseDato,
            lenke = hendelseLenke,
            detaljer = hendelseDetaljer,
        ),
    )
}

fun genererRandomHendelseConsumerRecord(
    key: String = UUID.randomUUID().toString(),
    recordValue: HendelseRecordValue = genererRandomHendelseRecordValue(),
    partition: Int = Random.nextInt(until = 5),
    offset: Long = Random.nextLong(until = 10_000),
): ConsumerRecord<String, HendelseRecordValue> {
    return ConsumerRecord(
        KafkaConfigCommon.Topic.PORTEFOLJE_HENDELSESFILTER.topicName,
        partition,
        offset,
        key,
        recordValue
    )
}

private fun randomUrl(): URL =
    URI.create("https://veilarbpersonflate.intern.dev.nav.no/${Random.nextInt(until = 10)}").toURL()

private fun randomBeskrivelse() = "Beskrivelse_${Random.nextInt(until = 10)}"

private fun randomAvsender() = "Avsender_${Random.nextInt(until = 10)}"

fun randomKategori(): Kategori {
    val kategoriValues = Kategori.entries
    return Random.nextInt(kategoriValues.size).let { kategoriValues[it] }
}

fun randomOperasjon(): Operasjon {
    val operasjonValues = Operasjon.entries
    return Random.nextInt(operasjonValues.size).let { operasjonValues[it] }
}

fun randomDetaljer(): String? {
    return if (Random.nextBoolean()) {
        "Detaljer_${Random.nextInt(until = 10)}"
    } else {
        null
    }
}
