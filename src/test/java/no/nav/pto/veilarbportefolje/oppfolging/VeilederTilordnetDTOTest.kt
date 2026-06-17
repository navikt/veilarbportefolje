package no.nav.pto.veilarbportefolje.oppfolging

import no.nav.common.json.JsonUtils
import no.nav.pto.veilarbportefolje.oppfolging.dto.VeilederTilordnetDTO
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class VeilederTilordnetDTOTest {
    @Test
    fun test_deserialization_when_veilederid_is_null() {
        val json = "{\"aktorId\": \"0000\",\"veilederId\": null}"

        val veilederTilordnetDTO = JsonUtils.fromJson(json, VeilederTilordnetDTO::class.java)
        Assertions.assertEquals(veilederTilordnetDTO.aktorId.toString(), "0000")
        Assertions.assertNull(veilederTilordnetDTO.veilederId?.value)
    }
}
