package no.nav.pto.veilarbportefolje.skjerming

import no.nav.common.json.JsonUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SkjermingDTOTest {
    @Test
    fun testDeserialization() {
        val json = """
        {
          "skjermetFra": [2022, 3, 23, 14, 53, 54],
          "skjermetTil": null
        }
        """.trimIndent()

        try {
            val skjermingDTO = JsonUtils.fromJson(json, SkjermingDTO::class.java)

            assertEquals(skjermingDTO.skjermetFra?.size, 6)
            assertNull(skjermingDTO.skjermetTil)
        } catch (_: Exception) {
            fail()
        }
    }
}
