package no.nav.pto.veilarbportefolje.oppfolging

import no.nav.common.json.JsonUtils
import no.nav.pto.veilarbportefolje.oppfolging.dto.VeilederTilordnetDTO
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

internal class VeilederTilordnetDTOTest {
    @Test
    fun test_deserialization_when_veilederid_is_null() {
        val json = "{\"aktorId\": \"0000\", \"veilederId\": null}"

        val veilederTilordnetDTO = JsonUtils.fromJson(json, VeilederTilordnetDTO::class.java)
        Assertions.assertEquals(veilederTilordnetDTO.aktorId.toString(), "0000")
        Assertions.assertNull(veilederTilordnetDTO.veilederId?.value)
    }

    @Test
    fun test_deserialization() {
        val json =
            "{\"aktorId\": \"11111111111\", \"veilederId\": \"Z999999\", \"tilordnetTidspunkt\": \"2026-06-17T12:00:00+02:00\"}"

        val veilederTilordnetDTO = JsonUtils.fromJson(json, VeilederTilordnetDTO::class.java)
        Assertions.assertEquals("11111111111", veilederTilordnetDTO.aktorId.toString())
        Assertions.assertEquals("Z999999", veilederTilordnetDTO.veilederId?.value)
        Assertions.assertEquals(
            ZonedDateTime.parse("2026-06-17T12:00:00+02:00"),
            veilederTilordnetDTO.tilordnetTidspunkt
        )
    }
}
