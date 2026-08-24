package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.NorskIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.net.URI
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

/**
 * Tester for serialisering, deserialisering og mapping mellom typer.
 */
class DataformatTest {

    @Test
    fun `deserialisering av JSON payload gir forventet HendelseRecordValue`() {
        // language=json
        val jsonInput = """
            {
              "personID": "11111199999",
              "avsender": "veilarbdialog",
              "kategori": "UTGATT_VARSEL",
              "operasjon": "START",
              "hendelse": {
                "beskrivelse": "Bruker har et utgått varsel",
                "beskrivelseEnum": null,
                "dato": "2024-11-27T00:00:00.000+01:00",
                "datoFrist": "2024-12-04T00:00:00.000+01:00",
                "lenke": "https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan",
                "detaljer": null
              }
            }
        """.trimIndent()

        val deserialisertHendelseRecordValue = JsonUtils.fromJson(jsonInput, HendelseRecordValue::class.java)

        val forventetHendelseRecordValue = HendelseRecordValue(
            personID = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValue.HendelseInnhold(
                beskrivelse = "Bruker har et utgått varsel",
                beskrivelseEnum = null,
                dato = ZonedDateTime.of(2024, 11, 27, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                datoFrist = ZonedDateTime.of(2024, 12, 4, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                lenke = URI.create("https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan").toURL(),
                detaljer = null,
            ),
        )
        assertThat(deserialisertHendelseRecordValue).isNotNull
        assertThat(deserialisertHendelseRecordValue).isEqualTo(forventetHendelseRecordValue)
  }

    @Test
    fun `deserialisering av JSON payload med beskrivelseEnum gir forventet HendelseRecordValue`() {
        // language=json
        val jsonInput = """
            {
              "personID": "11111199999",
              "avsender": "veilarbdialog",
              "kategori": "UTGATT_VARSEL",
              "operasjon": "START",
              "hendelse": {
                "beskrivelse": "Bruker har et utgått varsel",
                "beskrivelseEnum": "UTGATT_VARSEL_28_DAGER",
                "dato": "2024-11-27T00:00:00.000+01:00",
                "lenke": "https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan",
                "detaljer": null
              }
            }
        """.trimIndent()

        val deserialisertHendelseRecordValue = JsonUtils.fromJson(jsonInput, HendelseRecordValue::class.java)

        val forventetHendelseRecordValue = HendelseRecordValue(
            personID = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValue.HendelseInnhold(
                beskrivelse = "Bruker har et utgått varsel",
                beskrivelseEnum = "UTGATT_VARSEL_28_DAGER",
                dato = ZonedDateTime.of(2024, 11, 27, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                lenke = URI.create("https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan").toURL(),
                detaljer = null,
            ),
        )
        assertThat(deserialisertHendelseRecordValue).isNotNull
        assertThat(deserialisertHendelseRecordValue).isEqualTo(forventetHendelseRecordValue)
    }

    @Test
    fun `deserialisering av JSON payload uten beskrivelseEnum og datoFrist gir forventet HendelseRecordValue (bakoverkompatibilitet)`() {
        // language=json
        val jsonInput = """
            {
              "personID": "11111199999",
              "avsender": "veilarbdialog",
              "kategori": "UTGATT_VARSEL",
              "operasjon": "START",
              "hendelse": {
                "beskrivelse": "Bruker har et utgått varsel",
                "dato": "2024-11-27T00:00:00.000+01:00",
                "lenke": "https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan",
                "detaljer": null
              }
            }
        """.trimIndent()

        val deserialisertHendelseRecordValue = JsonUtils.fromJson(jsonInput, HendelseRecordValue::class.java)

        val forventetHendelseRecordValue = HendelseRecordValue(
            personID = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValue.HendelseInnhold(
                beskrivelse = "Bruker har et utgått varsel",
                beskrivelseEnum = null,
                dato = ZonedDateTime.of(2024, 11, 27, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                datoFrist = null,
                lenke = URI.create("https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan").toURL(),
                detaljer = null,
            ),
        )
        assertThat(deserialisertHendelseRecordValue).isNotNull
        assertThat(deserialisertHendelseRecordValue).isEqualTo(forventetHendelseRecordValue)
    }

    @Test
    fun `serialisering av HendelseRecordValue gir forventet JSON`() {
        val hendelseRecordValueInput = HendelseRecordValue(
            personID = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValue.HendelseInnhold(
                beskrivelse = "Bruker har et utgått varsel",
                beskrivelseEnum = null,
                dato = ZonedDateTime.of(2024, 11, 27, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                datoFrist = null,
                lenke = URI.create("https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan").toURL(),
                detaljer = null,
            ),
        )

        val serialisertJson = JsonUtils.toJson(hendelseRecordValueInput)

        // language=json
        val forventetJson = """
            {
              "personID": "11111199999",
              "avsender": "veilarbdialog",
              "kategori": "UTGATT_VARSEL",
              "operasjon": "START",
              "hendelse": {
                "beskrivelse": "Bruker har et utgått varsel",
                "beskrivelseEnum": null,
                "dato": "2024-11-27T00:00:00+01:00",
                "datoFrist": null,
                "lenke": "https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan",
                "detaljer": null
              }
            }
        """.trimIndent()
        assertThat(serialisertJson).isNotNull
        JSONAssert.assertEquals(forventetJson, serialisertJson, false)
    }

    @Test
    fun `toHendelse gir forventet Hendelse`() {
        val hendelseID = "96463d56-019e-4b30-ae9b-7365cf002a09"
        val hendelseRecordValueInput = HendelseRecordValue(
            personID = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValue.HendelseInnhold(
                beskrivelse = "Bruker har et utgått varsel",
                beskrivelseEnum = null,
                dato = ZonedDateTime.of(2024, 11, 27, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                datoFrist = ZonedDateTime.of(2024, 12, 4, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                lenke = URI.create("https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan").toURL(),
                detaljer = null,
            ),
        )

        val mappedHendelse = toHendelse(hendelseRecordValueInput, hendelseID)

        val forventetHendeles = Hendelse(
            id = UUID.fromString("96463d56-019e-4b30-ae9b-7365cf002a09"),
            personIdent = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            hendelse = Hendelse.HendelseInnhold(
                beskrivelse = "Bruker har et utgått varsel",
                beskrivelseEnum = null,
                dato = ZonedDateTime.of(2024, 11, 27, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                datoFrist = ZonedDateTime.of(2024, 12, 4, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                lenke = URI.create("https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan").toURL(),
                detaljer = null,
            ),
        )
        assertThat(mappedHendelse).isNotNull
        assertThat(mappedHendelse).isEqualTo(forventetHendeles)
    }
}
