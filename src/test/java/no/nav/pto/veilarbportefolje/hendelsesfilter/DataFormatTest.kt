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
    fun `skal kunne lese både eksisterende og nytt schema fra topic-payload`() {
        // language=json
        val eksisterendeMelding = """
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

        // language=json
        val nyMelding = """
            {
              "personID": "11111199999",
              "avsender": "veilarbdialog",
              "kategori": "UTGATT_VARSEL",
              "operasjon": "START",
              "schemaVersjon": "V2",
              "hendelse": {
                "beskrivelse": "Bruker har et utgått varsel",
                "tidspunkt": "2024-11-27T00:00:00.000+01:00",
                "lenke": "https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan",
                "detaljer": null
              }
            }
        """.trimIndent()

        val deserialisertEksisterende = JsonUtils.fromJson(eksisterendeMelding, HendelseRecordValue::class.java)
        val deserialisertNy = JsonUtils.fromJson(nyMelding, HendelseRecordValue::class.java)

        assertThat(deserialisertEksisterende).isInstanceOf(HendelseRecordValueV1::class.java)
        assertThat(deserialisertNy).isInstanceOf(HendelseRecordValueV2::class.java)
    }

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

        val forventetHendelseRecordValue = HendelseRecordValueV1(
            personID = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValueV1.HendelseInnhold(
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
    fun `deserialisering av JSON payload med schemaVersjon V2 gir HendelseRecordValueV2`() {
        // language=json
        val jsonInput = """
            {
              "personID": "11111199999",
              "avsender": "veilarbdialog",
              "kategori": "UTGATT_VARSEL",
              "operasjon": "START",
              "schemaVersjon": "V2",
              "hendelse": {
                "beskrivelse": "Bruker har et utgått varsel",
                "beskrivelseEnum": "UTGATT_VARSEL_28_DAGER",
                "tidspunkt": "2024-11-27T00:00:00.000+01:00",
                "tidspunktFrist": "2024-12-04T00:00:00.000+01:00",
                "lenke": "https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan",
                "detaljer": null
              }
            }
        """.trimIndent()

        val deserialisertHendelseRecordValue = JsonUtils.fromJson(jsonInput, HendelseRecordValue::class.java)

        val forventetHendelseRecordValue = HendelseRecordValueV2(
            personID = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValueV2.HendelseInnhold(
                beskrivelse = "Bruker har et utgått varsel",
                beskrivelseEnum = "UTGATT_VARSEL_28_DAGER",
                tidspunkt = ZonedDateTime.of(2024, 11, 27, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                tidspunktFrist = ZonedDateTime.of(2024, 12, 4, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                lenke = URI.create("https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan").toURL(),
                detaljer = null,
            ),
        )

        assertThat(deserialisertHendelseRecordValue).isNotNull
        assertThat(deserialisertHendelseRecordValue).isEqualTo(forventetHendelseRecordValue)
    }

    @Test
    fun `deserialisering av schemaVersjon V2 STOPP-melding med null hendelse er støttet`() {
        // language=json
        val jsonInput = """
            {
              "personID": "11111199999",
              "avsender": "veilarbdialog",
              "kategori": "UTGATT_VARSEL",
              "operasjon": "STOPP",
              "schemaVersjon": "V2",
              "hendelse": null
            }
        """.trimIndent()

        val deserialisertHendelseRecordValue = JsonUtils.fromJson(jsonInput, HendelseRecordValue::class.java)

        assertThat(deserialisertHendelseRecordValue).isInstanceOf(HendelseRecordValueV2::class.java)
        val asV2 = deserialisertHendelseRecordValue as HendelseRecordValueV2
        assertThat(asV2.hendelse).isNull()
        assertThat(asV2.operasjon).isEqualTo(Operasjon.STOPP)
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

        val forventetHendelseRecordValue = HendelseRecordValueV1(
            personID = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValueV1.HendelseInnhold(
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

        val forventetHendelseRecordValue = HendelseRecordValueV1(
            personID = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValueV1.HendelseInnhold(
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
        val hendelseRecordValueInput = HendelseRecordValueV1(
            personID = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValueV1.HendelseInnhold(
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
        val hendelseRecordValueInput = HendelseRecordValueV1(
            personID = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValueV1.HendelseInnhold(
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

    @Test
    fun `toHendelse mapper HendelseRecordValueV2-felt til intern modell`() {
        val hendelseID = "96463d56-019e-4b30-ae9b-7365cf002a09"
        val hendelseRecordValueInput = HendelseRecordValueV2(
            personID = NorskIdent("11111199999"),
            avsender = "veilarbdialog",
            kategori = Kategori.UTGATT_VARSEL,
            operasjon = Operasjon.START,
            hendelse = HendelseRecordValueV2.HendelseInnhold(
                beskrivelse = "Bruker har et utgått varsel",
                beskrivelseEnum = null,
                tidspunkt = ZonedDateTime.of(2024, 11, 27, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
                tidspunktFrist = ZonedDateTime.of(2024, 12, 4, 0, 0, 0, 0, ZoneOffset.of("+01:00")),
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
