package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.common.json.JsonUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HendelseServiceTest {

    @Test
    fun `kan deserialisere fra JSON til HendelseRecordValue`() {
        // language=json
        val json = """
            {
              "personID": "96463d56-019e-4b30-ae9b-7365cf002a09",
              "avsender": "dev-gcp:dab:aktivitetsplan",
              "kategori": "UTGATT_VARSEL",
              "operasjon": "START",
              "hendelse": {
                "navn": "Bruker har et utgått varsel",
                "dato": "2024-11-27T19:37:08.234614+01:00",
                "lenke": "https://veilarbpersonflate.intern.dev.nav.no/aktivitetsplan",
                "detaljer": null
              }
            }
        """.trimIndent()

        val deserialisertHendelseRecordValue = JsonUtils.fromJson(json, HendelseRecordValue::class.java)
        assertThat(deserialisertHendelseRecordValue).isNotNull
    }
}