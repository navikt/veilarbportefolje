package no.nav.pto.veilarbportefolje.hendelsesfilter

import Hendelse
import Kategori
import no.nav.common.types.identer.NorskIdent
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNorskIdent
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomZonedDate
import java.net.URI
import java.net.URL
import java.time.ZonedDateTime
import java.util.*
import kotlin.random.Random

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

private fun randomUrl(): URL =
    URI.create("https://veilarbpersonflate.intern.dev.nav.no/${Random.nextInt(until = 10)}").toURL()

private fun randomBeskrivelse() = "Beskrivelse_${Random.nextInt(until = 10)}"

private fun randomAvsender() = "Avsender_${Random.nextInt(until = 10)}"

fun randomKategori(): Kategori {
    val kategoriValues = Kategori.entries
    return Random.nextInt(kategoriValues.size).let { kategoriValues[it] }
}

fun randomDetaljer(): String? {
    return if (Random.nextBoolean()) {
        "Detaljer_${Random.nextInt(until = 10)}"
    } else {
        null
    }
}
