package no.nav.pto.veilarbportefolje.lagredefilter

import no.nav.pto.veilarbportefolje.domene.Kjonn
import no.nav.pto.veilarbportefolje.domene.filtervalg.AktivitetFiltervalg
import no.nav.pto.veilarbportefolje.domene.getFiltervalgDefaults
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.ekstraherAktiveFiltervalg
import no.nav.pto.veilarbportefolje.lagredefilter.minefilter.rekonstruerFiltervalgFraAktive
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AktiveFiltervalgTest {

    @Test
    fun `defaults skal gi tom object node`() {
        val aktive = ekstraherAktiveFiltervalg(getFiltervalgDefaults())

        assertThat(aktive.isEmpty).isTrue()
    }

    @Test
    fun `felter som avviker fra default skal beholdes`() {
        val filtervalg = getFiltervalgDefaults().copy(
            navnEllerFnrQuery = "Ola Nordmann",
            veiledere = listOf("Z123456", "Z654321"),
            kjonn = Kjonn.K
        )

        val aktive = ekstraherAktiveFiltervalg(filtervalg)

        assertThat(aktive.propertyNames()).containsExactlyInAnyOrder(
            "navnEllerFnrQuery", "veiledere", "kjonn"
        )
        assertThat(aktive.get("navnEllerFnrQuery").asString()).isEqualTo("Ola Nordmann")
        assertThat(aktive.get("kjonn").asString()).isEqualTo("K")
        val veiledere = aktive.get("veiledere")
        assertThat(veiledere).hasSize(2)
        assertThat(veiledere[0].asString()).isEqualTo("Z123456")
        assertThat(veiledere[1].asString()).isEqualTo("Z654321")
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

        assertThat(aktive.has("aktiviteter")).isFalse()
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

        val lagretAktiviteter = aktive.get("aktiviteter")
        assertThat(lagretAktiviteter).isNotNull
        assertThat(lagretAktiviteter.propertyNames())
            .containsExactlyInAnyOrder("MOTE", "EGEN", "BEHANDLING")
        assertThat(lagretAktiviteter.get("MOTE").asString()).isEqualTo("JA")
        assertThat(lagretAktiviteter.get("EGEN").asString()).isEqualTo("NA")
    }

    @Test
    fun `rekonstruksjon av tom aktive-node skal gi defaults`() {
        val tom = ekstraherAktiveFiltervalg(getFiltervalgDefaults())

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
}
