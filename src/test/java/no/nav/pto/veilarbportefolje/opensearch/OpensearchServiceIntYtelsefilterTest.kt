package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.tiltakspenger.domene.TiltakspengerRettighet
import no.nav.pto.veilarbportefolje.util.DateUtils
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.*

class OpensearchServiceIntYtelsefilterTest @Autowired constructor(
    private val opensearchService: OpensearchService,
) : EndToEndTest() {

    private lateinit var TEST_ENHET: String
    private lateinit var TEST_VEILEDER_0: String
    private lateinit var LITE_PRIVILEGERT_VEILEDER: String

    @BeforeEach
    fun setup() {
        TEST_ENHET = randomNavKontor().value
        TEST_VEILEDER_0 = randomVeilederId().value
        LITE_PRIVILEGERT_VEILEDER = randomVeilederId().value
    }

    @Test
    fun skal_hente_ut_brukere_som_gaar_paa_arbeidsavklaringspenger_som_rettighetsgruppefilter() {
        val brukerMedAAP = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setRettighetsgruppekode(Rettighetsgruppe.AAP.name)

        val brukerUtenAAP = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setRettighetsgruppekode(Rettighetsgruppe.DAGP.name)


        val liste = listOf(brukerMedAAP, brukerUtenAAP)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setRettighetsgruppe(listOf(Rettighetsgruppe.AAP))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(1)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedAAP.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.doesNotContain(brukerUtenAAP.fnr)
    }

    @Test
    fun skal_hente_ut_brukere_som_gaar_paa_arbeidsavklaringspenger_behandlet_i_arena() {
        val brukerMedAAPUnntak = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setYtelse("AAP_UNNTAK")

        val brukerMedAAPOrdinar = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setYtelse("AAP_MAXTID")

        val brukerUtenAAP = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setYtelse("ORDINARE_DAGPENGER")


        val liste = listOf(brukerMedAAPOrdinar, brukerUtenAAP, brukerMedAAPUnntak)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelseAapArena(listOf(YtelseAapArena.HAR_AAP_ORDINAR, YtelseAapArena.HAR_AAP_UNNTAK))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(2)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedAAPUnntak.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedAAPOrdinar.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.doesNotContain(brukerUtenAAP.fnr)
    }

    @Test
    fun antall_brukere_med_arbeidsavklaringspenger_skal_vere_like_for_ytelsesfilter_og_aapArenafilter() {
        val brukerMedAAPUnntak = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setYtelse("AAP_UNNTAK")

        val brukerMedAAPOrdinar = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setYtelse("AAP_MAXTID")

        val brukerMedAAPUnntak2 = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setYtelse("AAP_UNNTAK")

        val brukerUtenAap = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setYtelse("TILTAKSPENGER")


        val liste = listOf(brukerMedAAPOrdinar, brukerMedAAPUnntak, brukerMedAAPUnntak2, brukerUtenAap)
        skrivBrukereTilTestindeks(liste)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValgAapArenaFilter = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelseAapArena(listOf(YtelseAapArena.HAR_AAP_ORDINAR, YtelseAapArena.HAR_AAP_UNNTAK))

        val filterValgYtelseFilter = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelse(YtelseFilterArena.AAP)

        val responseAapArenaFilter = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValgAapArenaFilter,
            null,
            null
        )

        val responseYtelseFilter = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValgYtelseFilter,
            null,
            null
        )
        Assertions.assertThat(responseAapArenaFilter.antall).isEqualTo(3)
        Assertions.assertThat(responseYtelseFilter.antall).isEqualTo(3)
        Assertions.assertThat(responseAapArenaFilter.brukere).extracting<String> { it.fnr }
            .containsExactlyInAnyOrder(
                brukerMedAAPUnntak.fnr,
                brukerMedAAPUnntak2.fnr,
                brukerMedAAPOrdinar.fnr
            )
        Assertions.assertThat(responseYtelseFilter.brukere).extracting<String> { it.fnr }
            .containsExactlyInAnyOrder(
                brukerMedAAPUnntak.fnr,
                brukerMedAAPUnntak2.fnr,
                brukerMedAAPOrdinar.fnr
            )
        Assertions.assertThat(responseAapArenaFilter.brukere).extracting<String> { it.fnr }
            .doesNotContain(
                brukerUtenAap.fnr
            )
        Assertions.assertThat(responseYtelseFilter.brukere).extracting<String> { it.fnr }
            .doesNotContain(
                brukerUtenAap.fnr
            )
    }

    @Test
    fun skal_hente_ut_brukere_som_gaar_paa_arbeidsavklaringspenger_behandlet_i_kelvin() {
        val brukerMedAAP = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setAap_kelvin(true)

        val brukerUtenAAP = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setAap_kelvin(false)


        val liste = listOf(brukerMedAAP, brukerUtenAAP)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelseAapKelvin(listOf(YtelseAapKelvin.HAR_AAP))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(1)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedAAP.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.doesNotContain(brukerUtenAAP.fnr)
    }

    @Test
    fun skal_hente_ut_brukere_som_gaar_paa_tiltakspenger_behandlet_i_tpsak() {
        val brukerMedTiltakspenger = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setTiltakspenger(true)

        val brukerUtenTiltakspenger = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setTiltakspenger(false)


        val liste = listOf(brukerMedTiltakspenger, brukerUtenTiltakspenger)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelseTiltakspenger(listOf(YtelseTiltakspenger.HAR_TILTAKSPENGER))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(1)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedTiltakspenger.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }
            .doesNotContain(brukerUtenTiltakspenger.fnr)
    }

    @Test
    fun skal_hente_ut_brukere_som_gaar_paa_tiltakspenger_behandlet_i_arena() {
        val brukerMedTiltakspenger = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setYtelse("TILTAKSPENGER")

        val brukerUtenTiltakspenger = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setYtelse("ORDINARE_DAGPENGER")


        val liste = listOf(brukerMedTiltakspenger, brukerUtenTiltakspenger)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelseTiltakspengerArena(listOf(YtelseTiltakspengerArena.HAR_TILTAKSPENGER))

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(1)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedTiltakspenger.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedTiltakspenger.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }
            .doesNotContain(brukerUtenTiltakspenger.fnr)
    }

    @Test
    fun antall_brukere_med_tiltakspenger_skal_vere_like_for_ytelsesfilter_og_tiltakspengerArenafilter() {
        val brukerMedTiltakspenger = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setYtelse("TILTAKSPENGER")

        val brukerUtenTiltakspenger = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setYtelse("DAGPENGER")


        val liste = listOf(brukerMedTiltakspenger, brukerUtenTiltakspenger)
        skrivBrukereTilTestindeks(liste)
        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValgTpArenaFilter = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelseTiltakspengerArena(listOf(YtelseTiltakspengerArena.HAR_TILTAKSPENGER))

        val filterValgYtelseFilter = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelse(YtelseFilterArena.TILTAKSPENGER)

        val responseTpArenaFilter = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValgTpArenaFilter,
            null,
            null
        )

        val responseYtelseFilter = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValgYtelseFilter,
            null,
            null
        )
        Assertions.assertThat(responseTpArenaFilter.antall).isEqualTo(1)
        Assertions.assertThat(responseYtelseFilter.antall).isEqualTo(1)
        Assertions.assertThat(responseTpArenaFilter.brukere).extracting<String> { it.fnr }
            .contains(brukerMedTiltakspenger.fnr)
        Assertions.assertThat(responseYtelseFilter.brukere).extracting<String> { it.fnr }
            .contains(brukerMedTiltakspenger.fnr)
        Assertions.assertThat(responseTpArenaFilter.brukere).extracting<String> { it.fnr }
            .doesNotContain(brukerUtenTiltakspenger.fnr)
        Assertions.assertThat(responseYtelseFilter.brukere).extracting<String> { it.fnr }
            .doesNotContain(brukerUtenTiltakspenger.fnr)
    }


    @Test
    fun skal_hente_ut_brukere_filtrert_paa_dagpenger_som_ytelse() {
        val brukerMedDagpengerMedPermittering = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setRettighetsgruppekode(Rettighetsgruppe.AAP.name)
            .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING.name)


        val brukerMedPermitteringFiskeindustri = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setRettighetsgruppekode(Rettighetsgruppe.AAP.name)
            .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI.name)

        val brukerMedAAP = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)
            .setRettighetsgruppekode(Rettighetsgruppe.DAGP.name)
            .setYtelse(YtelseMapping.AAP_MAXTID.name)

        val brukerMedAnnenVeileder = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().get())
            .setFnr(randomFnr().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
            .setRettighetsgruppekode(Rettighetsgruppe.AAP.name)
            .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI.name)

        val liste = listOf(
            brukerMedDagpengerMedPermittering,
            brukerMedPermitteringFiskeindustri,
            brukerMedAAP,
            brukerMedAnnenVeileder
        )
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelse(YtelseFilterArena.DAGPENGER)

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(2)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }
            .contains(brukerMedDagpengerMedPermittering.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }
            .contains(brukerMedPermitteringFiskeindustri.fnr)
    }

    @Test
    fun test_sortering_AAP() {
        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setYtelse("AAP_UNNTAK")
            .setUtlopsdato("2023-06-30T21:59:59Z")

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setEnhet_id(TEST_ENHET)
            .setYtelse("AAP_MAXTID")
            .setAapmaxtiduke(43)
            .setAapordinerutlopsdato(DateUtils.toLocalDateOrNull("2023-04-20"))

        val bruker3 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setYtelse("AAP_UNNTAK")
            .setEnhet_id(TEST_ENHET)

        val bruker4 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setNy_for_veileder(false)
            .setYtelse("AAP_MAXTID")
            .setEnhet_id(TEST_ENHET)

        val bruker5 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setYtelse("AAP_UNNTAK")
            .setUtlopsdato("2023-08-30T21:59:59Z")

        val bruker6 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setVeileder_id(TEST_VEILEDER_0)
            .setEnhet_id(TEST_ENHET)
            .setYtelse("AAP_MAXTID")
            .setAapmaxtiduke(12)
            .setAapordinerutlopsdato(DateUtils.toLocalDateOrNull("2023-04-12"))

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, bruker5, bruker6)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        var filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setYtelseAapArena(listOf(YtelseAapArena.HAR_AAP_ORDINAR))

        var response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.AAP_VURDERINGSFRIST,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(3)
        Assertions.assertThat(response.brukere[0].fnr).isEqualTo(bruker4.fnr)
        Assertions.assertThat(response.brukere[1].fnr).isEqualTo(bruker6.fnr)
        Assertions.assertThat(response.brukere[2].fnr).isEqualTo(bruker2.fnr)

        filterValg = Filtervalg()
            .setFerdigfilterListe(listOf())
            .setYtelseAapArena(listOf(YtelseAapArena.HAR_AAP_UNNTAK))

        response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.AAP_VURDERINGSFRIST,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(3)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[0].fnr, bruker3.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[1].fnr, bruker1.fnr)
        org.junit.jupiter.api.Assertions.assertEquals(response.brukere[2].fnr, bruker5.fnr)
    }

    @Test
    fun skal_sortere_brukere_pa_aap_tom_vedtaksdato() {
        val tidspunkt1 = LocalDate.now()
        val tidspunkt2 = LocalDate.now().plusDays(2)
        val tidspunkt3 = LocalDate.now().plusDays(3)

        val tidligstTomBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setAap_kelvin(true)
            .setAap_kelvin_tom_vedtaksdato(tidspunkt1)

        val midtImellomBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setAap_kelvin(true)
            .setAap_kelvin_tom_vedtaksdato(tidspunkt2)

        val senestTomBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setAap_kelvin(true)
            .setAap_kelvin_tom_vedtaksdato(tidspunkt3)

        val nullBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setAap_kelvin(false)


        val liste = listOf(midtImellomBruker, senestTomBruker, tidligstTomBruker, nullBruker)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelseAapKelvin(listOf(YtelseAapKelvin.HAR_AAP, YtelseAapKelvin.HAR_IKKE_AAP))

        val brukereMedAntall = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.AAP_KELVIN_TOM_VEDTAKSDATO,
            filtervalg,
            null,
            null
        )
        val brukereMedAntall2 = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.AAP_KELVIN_TOM_VEDTAKSDATO,
            filtervalg,
            null,
            null
        )

        val brukereStigende = brukereMedAntall.brukere
        val brukereSynkende = brukereMedAntall2.brukere

        Assertions.assertThat(brukereStigende.size).isEqualTo(4)
        Assertions.assertThat(brukereStigende[0].fnr).isEqualTo(tidligstTomBruker.fnr)
        Assertions.assertThat(brukereStigende[3].fnr).isEqualTo(nullBruker.fnr)

        Assertions.assertThat(brukereSynkende[0].fnr).isEqualTo(nullBruker.fnr)
        Assertions.assertThat(brukereSynkende[1].fnr).isEqualTo(senestTomBruker.fnr)
        Assertions.assertThat(brukereSynkende[3].fnr).isEqualTo(tidligstTomBruker.fnr)
    }


    @Test
    fun skal_sortere_brukere_pa_tiltakspenger_tom_vedtaksdato() {
        val tidspunkt1 = LocalDate.now()
        val tidspunkt2 = LocalDate.now().plusDays(2)
        val tidspunkt3 = LocalDate.now().plusDays(3)

        val tidligstTomBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setTiltakspenger(true)
            .setTiltakspenger_vedtaksdato_tom(tidspunkt1)

        val midtImellomBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setTiltakspenger(true)
            .setTiltakspenger_vedtaksdato_tom(tidspunkt2)

        val senestTomBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setTiltakspenger(true)
            .setTiltakspenger_vedtaksdato_tom(tidspunkt3)

        val nullBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setTiltakspenger(false)


        val liste = listOf(midtImellomBruker, senestTomBruker, tidligstTomBruker, nullBruker)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelseTiltakspenger(
                listOf(
                    YtelseTiltakspenger.HAR_TILTAKSPENGER,
                    YtelseTiltakspenger.HAR_IKKE_TILTAKSPENGER
                )
            )

        val brukereMedAntall = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.TILTAKSPENGER_VEDTAKSDATO_TOM,
            filtervalg,
            null,
            null
        )
        val brukereMedAntall2 = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.TILTAKSPENGER_VEDTAKSDATO_TOM,
            filtervalg,
            null,
            null
        )

        val brukereStigende = brukereMedAntall.brukere
        val brukereSynkende = brukereMedAntall2.brukere

        Assertions.assertThat(brukereStigende.size).isEqualTo(4)
        Assertions.assertThat(brukereStigende[0].fnr).isEqualTo(tidligstTomBruker.fnr)
        Assertions.assertThat(brukereStigende[3].fnr).isEqualTo(nullBruker.fnr)

        Assertions.assertThat(brukereSynkende[0].fnr).isEqualTo(nullBruker.fnr)
        Assertions.assertThat(brukereSynkende[1].fnr).isEqualTo(senestTomBruker.fnr)
        Assertions.assertThat(brukereSynkende[3].fnr).isEqualTo(tidligstTomBruker.fnr)
    }

    @Test
    fun skal_sortere_brukere_pa_aap_rettighetstype() {
        val bistandsbehovBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setAap_kelvin(true)
            .setAap_kelvin_rettighetstype(AapRettighetstype.BISTANDSBEHOV)

        val studentBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setAap_kelvin(true)
            .setAap_kelvin_rettighetstype(AapRettighetstype.STUDENT)

        val sykepengeerstatningBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setAap_kelvin(true)
            .setAap_kelvin_rettighetstype(AapRettighetstype.SYKEPENGEERSTATNING)

        val nullBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setAap_kelvin(false)


        val liste = listOf(sykepengeerstatningBruker, bistandsbehovBruker, studentBruker, nullBruker)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelseAapKelvin(listOf(YtelseAapKelvin.HAR_AAP, YtelseAapKelvin.HAR_IKKE_AAP))

        val brukereMedAntall = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.AAP_KELVIN_RETTIGHETSTYPE,
            filtervalg,
            null,
            null
        )
        val brukereMedAntall2 = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.AAP_KELVIN_RETTIGHETSTYPE,
            filtervalg,
            null,
            null
        )

        val brukereStigende = brukereMedAntall.brukere
        val brukereSynkende = brukereMedAntall2.brukere

        Assertions.assertThat(brukereStigende.size).isEqualTo(4)
        Assertions.assertThat(brukereStigende[0].fnr).isEqualTo(bistandsbehovBruker.fnr)
        Assertions.assertThat(brukereStigende[1].fnr).isEqualTo(studentBruker.fnr)
        Assertions.assertThat(brukereStigende[3].fnr).isEqualTo(nullBruker.fnr)


        Assertions.assertThat(brukereSynkende[0].fnr).isEqualTo(sykepengeerstatningBruker.fnr)
        Assertions.assertThat(brukereSynkende[2].fnr).isEqualTo(bistandsbehovBruker.fnr)
        Assertions.assertThat(brukereSynkende[3].fnr).isEqualTo(nullBruker.fnr)
    }

    @Test
    fun skal_sortere_brukere_pa_tiltakspenger_rettighet() {
        val bruker1 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setTiltakspenger(true)
            .setTiltakspenger_rettighet(TiltakspengerRettighet.TILTAKSPENGER)

        val bruker2 = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setTiltakspenger(true)
            .setTiltakspenger_rettighet(TiltakspengerRettighet.TILTAKSPENGER_OG_BARNETILLEGG)

        val nullBruker = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().toString())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setTiltakspenger(false)


        val liste = listOf(bruker1, bruker2, nullBruker)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = Filtervalg()
            .setFerdigfilterListe(emptyList())
            .setYtelseTiltakspenger(
                listOf(
                    YtelseTiltakspenger.HAR_TILTAKSPENGER,
                    YtelseTiltakspenger.HAR_IKKE_TILTAKSPENGER
                )
            )

        val brukereMedAntall = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.TILTAKSPENGER_RETTIGHET,
            filtervalg,
            null,
            null
        )
        val brukereMedAntall2 = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.TILTAKSPENGER_RETTIGHET,
            filtervalg,
            null,
            null
        )

        val brukereStigende = brukereMedAntall.brukere
        val brukereSynkende = brukereMedAntall2.brukere

        Assertions.assertThat(brukereStigende.size).isEqualTo(3)
        Assertions.assertThat(brukereStigende[0].fnr).isEqualTo(bruker1.fnr)
        Assertions.assertThat(brukereStigende[1].fnr).isEqualTo(bruker2.fnr)
        Assertions.assertThat(brukereStigende[2].fnr).isEqualTo(nullBruker.fnr)

        Assertions.assertThat(brukereSynkende[0].fnr).isEqualTo(bruker2.fnr)
        Assertions.assertThat(brukereSynkende[2].fnr).isEqualTo(nullBruker.fnr)
    }


    private fun skrivBrukereTilTestindeks(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(indexName.value, listOf(*brukere.toTypedArray()))
    }
}
