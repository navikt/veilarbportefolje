package no.nav.pto.veilarbportefolje.domene

import no.nav.pto.veilarbportefolje.auth.BrukerinnsynTilganger
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModellMapper
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class SensurerBrukerTest {
    private val ingenTilganger = BrukerinnsynTilganger(false, false, false)
    private val alleTilganger = BrukerinnsynTilganger(true, true, true)

    @Test
    fun skalIkkeSeKode6Bruker() {
        sjekkAtKonfidensiellDataErVasket(sensurer(kode6Bruker(), BrukerinnsynTilganger(false, true, true)))
    }

    @Test
    fun skalIkkeSeKode7Bruker() {
        sjekkAtKonfidensiellDataErVasket(sensurer(kode7Bruker(), BrukerinnsynTilganger(true, false, true)))
    }

    @Test
    fun skalIkkeSeEgenAnsatt() {
        sjekkAtKonfidensiellDataErVasket(sensurer(egenAnsatt(), BrukerinnsynTilganger(true, true, false)))
    }

    @Test
    fun skalIkkeSeKode7BrukerSomOgsaErEgenAnsattUtenSkjermingTilgang() {
        val bruker = kode7Bruker().apply { egen_ansatt = true }
        sjekkAtKonfidensiellDataErVasket(sensurer(bruker, BrukerinnsynTilganger(false, true, false)))
    }

    @Test
    fun skalSeKode6Bruker() {
        val bruker = sensurer(kode6Bruker(), BrukerinnsynTilganger(true, false, false))
        assertThat(bruker.fnr).isEqualTo("11111111111")
        assertThat(bruker.fornavn).isEqualTo("fornavnKode6")
        assertThat(bruker.etternavn).isEqualTo("etternanvKode6")
    }

    @Test
    fun skalSeKode7Bruker() {
        val bruker = sensurer(kode7Bruker(), BrukerinnsynTilganger(false, true, false))
        assertThat(bruker.fnr).isEqualTo("11111111111")
        assertThat(bruker.fornavn).isEqualTo("fornavnKode7")
        assertThat(bruker.etternavn).isEqualTo("etternanvKode7")
    }

    @Test
    fun skalSeEgenAnsatt() {
        val bruker = sensurer(egenAnsatt(), BrukerinnsynTilganger(false, false, true))
        assertThat(bruker.fnr).isEqualTo("11111111111")
        assertThat(bruker.fornavn).isEqualTo("fornavnKodeEgenAnsatt")
        assertThat(bruker.etternavn).isEqualTo("etternanvEgenAnsatt")
        assertThat(bruker.skjermetTil).isNotNull()
        assertThat(bruker.etiketter.skjermetTil).isNotNull()
    }

    @Test
    fun skalSeIkkeKonfidensiellBruker() {
        val bruker = sensurer(ikkeKonfidensiellBruker(), ingenTilganger)
        assertThat(bruker.fnr).isEqualTo("11111111111")
        assertThat(bruker.fornavn).isEqualTo("fornavnIkkeKonfidensiellBruker")
        assertThat(bruker.etternavn).isEqualTo("etternanvIkkeKonfidensiellBruker")
    }

    @Test
    fun skalIkkeSeKode19BrukerUtenKode6Tilgang() {
        val bruker = sensurer(ikkeKonfidensiellBruker().apply { diskresjonskode = "19" }, ingenTilganger)
        assertThat(bruker.fnr).isEmpty()
    }

    @Test
    fun skalSeKode19BrukerMedKode6Tilgang() {
        val bruker = sensurer(ikkeKonfidensiellBruker().apply { diskresjonskode = "19" }, BrukerinnsynTilganger(true, false, false))
        assertThat(bruker.fnr).isEqualTo("11111111111")
    }

    @Test
    fun skalIkkeSeKode6Barn() {
        val bruker = sensurer(brukerMedKode6Barn(), BrukerinnsynTilganger(false, true, true))
        assertThat(bruker.barnUnder18AarData).isEmpty()
    }

    @Test
    fun skalIkkeSeKode7Barn() {
        val bruker = sensurer(brukerMedKode7Barn(), BrukerinnsynTilganger(true, false, true))
        assertThat(bruker.barnUnder18AarData).isEmpty()
    }

    @Test
    fun skalFjerneKode7BarnMenIkkeKode6() {
        val bruker = sensurer(brukerMedKode6og7Barn(), BrukerinnsynTilganger(true, false, false))
        assertThat(bruker.barnUnder18AarData).extracting<String?> { it.diskresjonskode }
            .containsExactlyInAnyOrder("6", null)
    }

    @Test
    fun skalIkkeSeKode19Barn() {
        val bruker = sensurer(brukerMedKode19Barn(), BrukerinnsynTilganger(false, true, true))
        assertThat(bruker.barnUnder18AarData).hasSize(2)
            .allSatisfy { assertThat(it.diskresjonskode).isNotEqualTo("19") }
    }

    @Test
    fun skalSeKode19Barn() {
        val bruker = sensurer(brukerMedKode19Barn(), alleTilganger)
        assertThat(bruker.barnUnder18AarData).hasSize(3)
            .anySatisfy { assertThat(it.diskresjonskode).isEqualTo("19") }
    }

    private fun sensurer(
        bruker: PortefoljebrukerOpensearchModell,
        tilganger: BrukerinnsynTilganger
    ): PortefoljebrukerFrontendModell =
        PortefoljebrukerFrontendModellMapper.toSensurertPortefoljebrukerFrontendModell(
            opensearchBruker = bruker,
            ufordelt = false,
            filtervalg = getFiltervalgDefaults(),
            brukerinnsynTilganger = tilganger
        )

    private fun sjekkAtKonfidensiellDataErVasket(bruker: PortefoljebrukerFrontendModell) {
        assertThat(bruker.fnr).isEqualTo("")
        assertThat(bruker.etternavn).isEqualTo("")
        assertThat(bruker.fornavn).isEqualTo("")
        assertThat(bruker.skjermetTil).isNull()
        assertThat(bruker.etiketter.skjermetTil).isNull()
        assertThat(bruker.foedeland).isNull()
        assertThat(bruker.tolkebehov.talespraaktolk).isEqualTo("")
        assertThat(bruker.tolkebehov.tegnspraaktolk).isEqualTo("")
        assertThat(bruker.hovedStatsborgerskap).isNull()
        assertThat(bruker.geografiskBosted.bostedBydel).isNull()
        assertThat(bruker.geografiskBosted.bostedKommune).isNull()
        assertThat(bruker.geografiskBosted.bostedKommuneUkjentEllerUtland).isEqualTo("-")
        assertThat(bruker.fargekategori).isNull()
        assertThat(bruker.huskelapp).isNull()
    }

    private fun bruker(fornavn: String = "fornavn", etternavn: String = "etternavn") =
        PortefoljebrukerOpensearchModell(
            fnr = "11111111111",
            fornavn = fornavn,
            etternavn = etternavn,
            kommunenummer = "0301",
            bydelsnummer = "030101",
            foedelandFulltNavn = "Norge",
            talespraaktolk = "EN",
            fargekategori = "FARGEKATEGORI_A",
            skjermet_til = LocalDateTime.now().plusDays(10),
        )

    private fun kode6Bruker() = bruker("fornavnKode6", "etternanvKode6").apply { diskresjonskode = "6" }

    private fun kode7Bruker() = bruker("fornavnKode7", "etternanvKode7").apply { diskresjonskode = "7" }

    private fun egenAnsatt() = bruker("fornavnKodeEgenAnsatt", "etternanvEgenAnsatt").apply { egen_ansatt = true }

    private fun ikkeKonfidensiellBruker() = bruker("fornavnIkkeKonfidensiellBruker", "etternanvIkkeKonfidensiellBruker")

    private fun brukerMedKode6Barn() = bruker().apply {
        barn_under_18_aar = listOf(BarnUnder18AarData(15, "6"), BarnUnder18AarData(12, "6"))
    }

    private fun brukerMedKode7Barn() = bruker().apply {
        barn_under_18_aar = listOf(BarnUnder18AarData(1, "7"))
    }

    private fun brukerMedKode19Barn() = bruker().apply {
        barn_under_18_aar = listOf(BarnUnder18AarData(15, "19"), BarnUnder18AarData(12, null), BarnUnder18AarData(3, null))
    }

    private fun brukerMedKode6og7Barn() = bruker().apply {
        barn_under_18_aar = listOf(BarnUnder18AarData(11, "6"), BarnUnder18AarData(15, "7"), BarnUnder18AarData(3, null))
    }
}
