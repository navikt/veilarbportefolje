package no.nav.pto.veilarbportefolje.lagredefilter

import no.nav.common.json.JsonUtils
import no.nav.pto.veilarbportefolje.domene.Kjonn
import no.nav.pto.veilarbportefolje.domene.filtervalg.AktivitetFiltervalg
import no.nav.pto.veilarbportefolje.domene.getFiltervalgDefaults
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.AktiveFiltervalg
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.ekstraherAktiveFiltervalg
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.rekonstruerFiltervalgFraAktive
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class AktiveFiltervalgTest {

    @Test
    fun `defaults skal serialiseres til tomt objekt`() {
        val aktive: AktiveFiltervalg = ekstraherAktiveFiltervalg(getFiltervalgDefaults())
        val json = JsonUtils.toJson(aktive)

        assertJsonEquals("{}", json)
    }

    @Test
    fun `felter som avviker fra default skal beholdes`() {
        val filtervalg = getFiltervalgDefaults().copy(
            navnEllerFnrQuery = "Ola Nordmann",
            veiledere = listOf("Z123456", "Z654321"),
            kjonn = Kjonn.K
        )

        val aktive = ekstraherAktiveFiltervalg(filtervalg)
        val json = JsonUtils.toJson(aktive)

        assertThat(aktive).isEqualTo(
            AktiveFiltervalg(
                navnEllerFnrQuery = "Ola Nordmann",
                veiledere = listOf("Z123456", "Z654321"),
                kjonn = Kjonn.K
            )
        )
        assertJsonEquals(
            """
                {
                  "navnEllerFnrQuery": "Ola Nordmann",
                  "veiledere": ["Z123456", "Z654321"],
                  "kjonn": "K"
                }
            """.trimIndent(),
            json
        )
    }

    @Test
    fun `aktiviteter skal skippes naar alle verdier er NA`() {
        val filtervalg = getFiltervalgDefaults().copy(
            aktiviteter = mapOf(
                "MOTE" to AktivitetFiltervalg.NA,
                "EGEN" to AktivitetFiltervalg.NA
            )
        )

        val aktive = ekstraherAktiveFiltervalg(filtervalg)

        assertThat(aktive.aktiviteter).isEmpty()
        assertJsonEquals("{}", JsonUtils.toJson(aktive))
    }

    @Test
    fun `aktiviteter skal beholdes i sin helhet naar minst en verdi er JA`() {
        val aktiviteter = mapOf(
            "MOTE" to AktivitetFiltervalg.JA,
            "EGEN" to AktivitetFiltervalg.NA,
            "BEHANDLING" to AktivitetFiltervalg.NA
        )
        val filtervalg = getFiltervalgDefaults().copy(aktiviteter = aktiviteter)

        val aktive = ekstraherAktiveFiltervalg(filtervalg)

        assertThat(aktive.aktiviteter).isEqualTo(aktiviteter)
        assertJsonEquals(
            """
                {
                  "aktiviteter": {
                    "MOTE": "JA",
                    "EGEN": "NA",
                    "BEHANDLING": "NA"
                  }
                }
            """.trimIndent(),
            JsonUtils.toJson(aktive)
        )
    }

    @Test
    fun `rekonstruksjon av tom aktive-dto skal gi defaults`() {
        val tom = AktiveFiltervalg()

        val rekonstruert = rekonstruerFiltervalgFraAktive(tom)

        assertThat(rekonstruert).isEqualTo(getFiltervalgDefaults())
    }

    @Test
    fun `roundtrip skal bevare alle satte verdier`() {
        val original = getFiltervalgDefaults().copy(
            navnEllerFnrQuery = "Kari",
            alder = listOf("20-24", "25-29"),
            kjonn = Kjonn.M,
            sisteEndringKategori = "AVTALT_MOTE",
            aktiviteter = mapOf(
                "MOTE" to AktivitetFiltervalg.JA,
                "EGEN" to AktivitetFiltervalg.NA
            )
        )

        val rekonstruert = rekonstruerFiltervalgFraAktive(ekstraherAktiveFiltervalg(original))

        assertThat(rekonstruert).isEqualTo(original)
    }

    @Test
    fun `roundtrip av kun defaults skal gi defaults`() {
        val defaults = getFiltervalgDefaults()

        val rekonstruert = rekonstruerFiltervalgFraAktive(ekstraherAktiveFiltervalg(defaults))

        assertThat(rekonstruert).isEqualTo(defaults)
    }

    private fun assertJsonEquals(expectedJson: String, actualJson: String) {
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT)
    }
}
