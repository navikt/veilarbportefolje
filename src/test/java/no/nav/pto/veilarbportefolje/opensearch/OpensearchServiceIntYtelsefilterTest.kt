package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerRettighetstype
import no.nav.pto.veilarbportefolje.domene.Sorteringsfelt
import no.nav.pto.veilarbportefolje.domene.Sorteringsrekkefolge
import no.nav.pto.veilarbportefolje.domene.YtelseMapping
import no.nav.pto.veilarbportefolje.domene.filtervalg.*
import no.nav.pto.veilarbportefolje.domene.getFiltervalgDefaults
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.DagpengerForOpensearch
import no.nav.pto.veilarbportefolje.opensearch.OpensearchConfig.BRUKERINDEKS_ALIAS
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
        val brukerMedAAP = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            rettighetsgruppekode = Rettighetsgruppe.AAP.name,
        )

        val brukerUtenAAP = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            rettighetsgruppekode = Rettighetsgruppe.DAGP.name,
        )


        val liste = listOf(brukerMedAAP, brukerUtenAAP)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults().copy(
            rettighetsgruppe = listOf(Rettighetsgruppe.AAP)
        )

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
        val brukerMedAAPUnntak = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            ytelse = "AAP_UNNTAK",
        )

        val brukerMedAAPOrdinar = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            ytelse = "AAP_MAXTID",
        )

        val brukerUtenAAP = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            ytelse = "ORDINARE_DAGPENGER",
        )


        val liste = listOf(brukerMedAAPOrdinar, brukerUtenAAP, brukerMedAAPUnntak)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            ytelseAapArena = listOf(YtelseAapArena.HAR_AAP_ORDINAR, YtelseAapArena.HAR_AAP_UNNTAK)
        )

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
    fun skal_hente_ut_brukere_som_gaar_paa_arbeidsavklaringspenger_behandlet_i_kelvin() {
        val brukerMedAAP = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            aap_kelvin = true,
        )

        val brukerUtenAAP = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            aap_kelvin = false,
        )


        val liste = listOf(brukerMedAAP, brukerUtenAAP)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            ytelseAapKelvin = listOf(YtelseAapKelvin.HAR_AAP)
        )

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
        val brukerMedTiltakspenger = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            tiltakspenger = true,
        )

        val brukerUtenTiltakspenger = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            tiltakspenger = false,
        )


        val liste = listOf(brukerMedTiltakspenger, brukerUtenTiltakspenger)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            ytelseTiltakspenger = listOf(YtelseTiltakspenger.HAR_TILTAKSPENGER)
        )

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
        val brukerMedTiltakspenger = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            ytelse = "TILTAKSPENGER",
        )

        val brukerUtenTiltakspenger = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            ytelse = "ORDINARE_DAGPENGER",
        )


        val liste = listOf(brukerMedTiltakspenger, brukerUtenTiltakspenger)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            ytelseTiltakspengerArena = listOf(YtelseTiltakspengerArena.HAR_TILTAKSPENGER)
        )

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
    fun skal_hente_ut_brukere_som_gaar_paa_dagpenger_behandlet_i_dpsak() {
        val dagpenger = DagpengerForOpensearch(
            harDagpenger = true,
            rettighetstype = DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER,
            antallResterendeDager = 100,
            datoAntallDagerBleBeregnet = LocalDate.now()
        )
        val dagpengerPermittering = DagpengerForOpensearch(
            harDagpenger = true,
            rettighetstype = DagpengerRettighetstype.DAGPENGER_PERMITTERING_ORDINAER,
            antallResterendeDager = 120,
            datoAntallDagerBleBeregnet = LocalDate.now()
        )
        val dagpengerPermitteringIkkeAktivYtelse = DagpengerForOpensearch(
            harDagpenger = false,
            rettighetstype = DagpengerRettighetstype.DAGPENGER_PERMITTERING_ORDINAER,
            antallResterendeDager = 0,
            datoAntallDagerBleBeregnet = LocalDate.now()
        )
        val dagpengerFiske = DagpengerForOpensearch(
            harDagpenger = true,
            rettighetstype = DagpengerRettighetstype.DAGPENGER_PERMITTERING_FISKEINDUSTRI,
            antallResterendeDager = 140,
            datoAntallDagerBleBeregnet = LocalDate.now()
        )

        val brukerMedDagpenger = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            dagpenger = dagpenger,
        )
        val brukerMedDagpengerPermittert = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            dagpenger = dagpengerPermittering,
        )
        val brukerMedDagpengerIkkeAktivYtelse = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            dagpenger = dagpengerPermitteringIkkeAktivYtelse,
        )

        val brukerMedDagpengerFiske = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            dagpenger = dagpengerFiske,
        )

        val brukerUtenDagpenger = PortefoljebrukerOpensearchModell(
            aktoer_id = randomAktorId().get(),
            fnr = randomFnr().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            veileder_id = TEST_VEILEDER_0,
            dagpenger = null
        )

        val liste = listOf(
            brukerMedDagpenger,
            brukerMedDagpengerPermittert,
            brukerMedDagpengerIkkeAktivYtelse,
            brukerMedDagpengerFiske,
            brukerUtenDagpenger
        )
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults().copy(
            ytelseDagpenger = listOf(
                YtelseDagpenger.HAR_DAGPENGER_ORDINAER,
                YtelseDagpenger.HAR_DAGPENGER_MED_PERMITTERING
            )
        )

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
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedDagpenger.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedDagpengerPermittert.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }
            .doesNotContain(brukerMedDagpengerFiske.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.doesNotContain(brukerUtenDagpenger.fnr)
    }


    @Test
    fun skal_hente_ut_brukere_som_gaar_paa_dagpenger_behandlet_i_arena() {
        val brukerMedDagpenger = PortefoljebrukerOpensearchModell()
        brukerMedDagpenger.aktoer_id = randomAktorId().get()
        brukerMedDagpenger.fnr = randomFnr().toString()
        brukerMedDagpenger.oppfolging = true
        brukerMedDagpenger.enhet_id = TEST_ENHET
        brukerMedDagpenger.veileder_id = TEST_VEILEDER_0
        brukerMedDagpenger.ytelse = YtelseMapping.ORDINARE_DAGPENGER.name

        val brukerMedDagpengerPerm = PortefoljebrukerOpensearchModell()
        brukerMedDagpengerPerm.aktoer_id = randomAktorId().get()
        brukerMedDagpengerPerm.fnr = randomFnr().toString()
        brukerMedDagpengerPerm.oppfolging = true
        brukerMedDagpengerPerm.enhet_id = TEST_ENHET
        brukerMedDagpengerPerm.veileder_id = TEST_VEILEDER_0
        brukerMedDagpengerPerm.ytelse = YtelseMapping.DAGPENGER_MED_PERMITTERING.name

        val brukerMedDagpengerFiske = PortefoljebrukerOpensearchModell()
        brukerMedDagpengerFiske.aktoer_id = randomAktorId().get()
        brukerMedDagpengerFiske.fnr = randomFnr().toString()
        brukerMedDagpengerFiske.oppfolging = true
        brukerMedDagpengerFiske.enhet_id = TEST_ENHET
        brukerMedDagpengerFiske.veileder_id = TEST_VEILEDER_0
        brukerMedDagpengerFiske.ytelse = YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI.name

        val brukerMedDagpengerLønn = PortefoljebrukerOpensearchModell()
        brukerMedDagpengerLønn.aktoer_id = randomAktorId().get()
        brukerMedDagpengerLønn.fnr = randomFnr().toString()
        brukerMedDagpengerLønn.oppfolging = true
        brukerMedDagpengerLønn.enhet_id = TEST_ENHET
        brukerMedDagpengerLønn.veileder_id = TEST_VEILEDER_0
        brukerMedDagpengerLønn.ytelse = YtelseMapping.LONNSGARANTIMIDLER_DAGPENGER.name

        val brukerUtenDagpenger = PortefoljebrukerOpensearchModell()
        brukerUtenDagpenger.aktoer_id = randomAktorId().get()
        brukerUtenDagpenger.fnr = randomFnr().toString()
        brukerUtenDagpenger.oppfolging = true
        brukerUtenDagpenger.enhet_id = TEST_ENHET
        brukerUtenDagpenger.veileder_id = TEST_VEILEDER_0
        brukerUtenDagpenger.ytelse = "TILTAKSPENGER"


        val liste = listOf(
            brukerMedDagpenger,
            brukerUtenDagpenger,
            brukerMedDagpengerPerm,
            brukerMedDagpengerFiske,
            brukerMedDagpengerLønn
        )
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filterValg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            ytelseDagpengerArena = listOf(
                YtelseDagpengerArena.HAR_DAGPENGER_ORDINAER,
                YtelseDagpengerArena.HAR_DAGPENGER_MED_PERMITTERING,
                YtelseDagpengerArena.HAR_DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI,
                YtelseDagpengerArena.HAR_DAGPENGER_LONNSGARANTIMIDLER
            )
        )

        val response = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.of(TEST_VEILEDER_0),
            Sorteringsrekkefolge.IKKE_SATT,
            Sorteringsfelt.IKKE_SATT,
            filterValg,
            null,
            null
        )

        Assertions.assertThat(response.antall).isEqualTo(4)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedDagpenger.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedDagpengerPerm.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedDagpengerFiske.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.contains(brukerMedDagpengerLønn.fnr)
        Assertions.assertThat(response.brukere).extracting<String> { it.fnr }.doesNotContain(brukerUtenDagpenger.fnr)
    }


    @Test
    fun test_sortering_AAP() {
        val bruker1 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
            ytelse = "AAP_UNNTAK",
            utlopsdato = "2023-06-30T21:59:59Z",
        )

        val bruker2 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            enhet_id = TEST_ENHET,
            ytelse = "AAP_MAXTID",
            aapmaxtiduke = 43,
            aapordinerutlopsdato = DateUtils.toLocalDateOrNull("2023-04-20"),
        )

        val bruker3 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            ytelse = "AAP_UNNTAK",
            enhet_id = TEST_ENHET,
        )

        val bruker4 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            ny_for_veileder = false,
            ytelse = "AAP_MAXTID",
            enhet_id = TEST_ENHET,
        )

        val bruker5 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
            ytelse = "AAP_UNNTAK",
            utlopsdato = "2023-08-30T21:59:59Z",
        )

        val bruker6 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            veileder_id = TEST_VEILEDER_0,
            enhet_id = TEST_ENHET,
            ytelse = "AAP_MAXTID",
            aapmaxtiduke = 12,
            aapordinerutlopsdato = DateUtils.toLocalDateOrNull("2023-04-12"),
        )

        val liste = listOf(bruker1, bruker2, bruker3, bruker4, bruker5, bruker6)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }


        var filterValg = getFiltervalgDefaults().copy(
            ferdigfilterListe = listOf(),
            ytelseAapArena = listOf(YtelseAapArena.HAR_AAP_ORDINAR)
        )

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

        filterValg = getFiltervalgDefaults().copy(
            ferdigfilterListe = listOf(),
            ytelseAapArena = listOf(YtelseAapArena.HAR_AAP_UNNTAK)
        )

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

        val tidligstTomBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            aap_kelvin = true,
            aap_kelvin_tom_vedtaksdato = tidspunkt1,
            aap_kelvin_rettighetstype = AapRettighetstype.SYKEPENGEERSTATNING
        )

        val midtImellomBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            aap_kelvin = true,
            aap_kelvin_tom_vedtaksdato = tidspunkt2,
            aap_kelvin_rettighetstype = AapRettighetstype.SYKEPENGEERSTATNING
        )

        val senestTomBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            aap_kelvin = true,
            aap_kelvin_tom_vedtaksdato = tidspunkt3,
            aap_kelvin_rettighetstype = AapRettighetstype.SYKEPENGEERSTATNING
        )

        val nullBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            aap_kelvin = false,
        )


        val liste = listOf(midtImellomBruker, senestTomBruker, tidligstTomBruker, nullBruker)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            ytelseAapKelvin = listOf(YtelseAapKelvin.HAR_AAP)
        )

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

        Assertions.assertThat(brukereStigende.size).isEqualTo(3)
        Assertions.assertThat(brukereStigende[0].fnr).isEqualTo(tidligstTomBruker.fnr)

        Assertions.assertThat(brukereSynkende[0].fnr).isEqualTo(senestTomBruker.fnr)
        Assertions.assertThat(brukereSynkende[2].fnr).isEqualTo(tidligstTomBruker.fnr)
    }


    @Test
    fun skal_sortere_brukere_pa_tiltakspenger_tom_vedtaksdato() {
        val tidspunkt1 = LocalDate.now()
        val tidspunkt2 = LocalDate.now().plusDays(2)
        val tidspunkt3 = LocalDate.now().plusDays(3)

        val tidligstTomBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            tiltakspenger = true,
            tiltakspenger_vedtaksdato_tom = tidspunkt1,
            tiltakspenger_rettighet = TiltakspengerRettighet.TILTAKSPENGER
        )

        val midtImellomBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            tiltakspenger = true,
            tiltakspenger_vedtaksdato_tom = tidspunkt2,
            tiltakspenger_rettighet = TiltakspengerRettighet.TILTAKSPENGER
        )

        val senestTomBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            tiltakspenger = true,
            tiltakspenger_vedtaksdato_tom = tidspunkt3,
            tiltakspenger_rettighet = TiltakspengerRettighet.TILTAKSPENGER
        )

        val nullBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            tiltakspenger = false,
            tiltakspenger_rettighet = TiltakspengerRettighet.TILTAKSPENGER
        )


        val liste = listOf(midtImellomBruker, senestTomBruker, tidligstTomBruker, nullBruker)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            ytelseTiltakspenger = listOf(YtelseTiltakspenger.HAR_TILTAKSPENGER)
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

        Assertions.assertThat(brukereStigende.size).isEqualTo(3)
        Assertions.assertThat(brukereStigende[0].fnr).isEqualTo(tidligstTomBruker.fnr)

        Assertions.assertThat(brukereSynkende[0].fnr).isEqualTo(senestTomBruker.fnr)
        Assertions.assertThat(brukereSynkende[2].fnr).isEqualTo(tidligstTomBruker.fnr)
    }

    @Test
    fun skal_sortere_brukere_pa_dagpenger_slutt_dato() {
        val tidspunkt1 = LocalDate.now()
        val tidspunkt2 = LocalDate.now().plusDays(2)
        val tidspunkt3 = LocalDate.now().plusDays(3)

        val arenaDagpengerBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            ytelse = YtelseMapping.ORDINARE_DAGPENGER.name
        )

        val ingenSluttdatoBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            dagpenger = DagpengerForOpensearch(
                true,
                DagpengerRettighetstype.DAGPENGER_PERMITTERING_ORDINAER,
                null,
                150,
                null
            )
        )

        val tidligstTomBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            dagpenger = DagpengerForOpensearch(
                true,
                DagpengerRettighetstype.DAGPENGER_PERMITTERING_ORDINAER,
                tidspunkt1,
                1,
                null
            )
        )

        val midtImellomBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            dagpenger = DagpengerForOpensearch(
                true,
                DagpengerRettighetstype.DAGPENGER_PERMITTERING_ORDINAER,
                tidspunkt2,
                2,
                null
            )
        )

        val senestTomBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            dagpenger = DagpengerForOpensearch(
                true,
                DagpengerRettighetstype.DAGPENGER_PERMITTERING_ORDINAER,
                tidspunkt3,
                3,
                null
            )
        )


        val liste = listOf(midtImellomBruker, senestTomBruker, tidligstTomBruker, ingenSluttdatoBruker, arenaDagpengerBruker)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = getFiltervalgDefaults().copy(
            ytelseDagpenger = listOf(YtelseDagpenger.HAR_DAGPENGER_MED_PERMITTERING),
            ytelseDagpengerArena = listOf(YtelseDagpengerArena.HAR_DAGPENGER_ORDINAER)
        )

        val brukereMedAntall = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.DAGPENGER_STANS,
            filtervalg,
            null,
            null
        )
        val brukereMedAntall2 = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.DAGPENGER_STANS,
            filtervalg,
            null,
            null
        )

        val brukereStigende = brukereMedAntall.brukere
        val brukereSynkende = brukereMedAntall2.brukere

        Assertions.assertThat(brukereStigende.size).isEqualTo(5)
        Assertions.assertThat(brukereStigende[0].fnr).isEqualTo(tidligstTomBruker.fnr)
        Assertions.assertThat(brukereStigende[1].fnr).isEqualTo(midtImellomBruker.fnr)
        Assertions.assertThat(brukereStigende[2].fnr).isEqualTo(senestTomBruker.fnr)
        Assertions.assertThat(brukereStigende[3].fnr).isEqualTo(ingenSluttdatoBruker.fnr)
        Assertions.assertThat(brukereStigende[4].fnr).isEqualTo(arenaDagpengerBruker.fnr)

        Assertions.assertThat(brukereSynkende[0].fnr).isEqualTo(ingenSluttdatoBruker.fnr)
        Assertions.assertThat(brukereSynkende[1].fnr).isEqualTo(senestTomBruker.fnr)
        Assertions.assertThat(brukereSynkende[2].fnr).isEqualTo(midtImellomBruker.fnr)
        Assertions.assertThat(brukereSynkende[3].fnr).isEqualTo(tidligstTomBruker.fnr)
        Assertions.assertThat(brukereStigende[4].fnr).isEqualTo(arenaDagpengerBruker.fnr)

    }


    @Test
    fun skal_sortere_brukere_pa_dagpenger_antall_dager_og_rettighetstype() {
        val tidspunkt1 = LocalDate.now()
        val tidspunkt2 = LocalDate.now().plusDays(2)
        val tidspunkt3 = LocalDate.now().plusDays(3)

        val bruker1 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            dagpenger = DagpengerForOpensearch(
                true,
                DagpengerRettighetstype.DAGPENGER_PERMITTERING_ORDINAER,
                null,
                null,
                null
            )
        )

        val bruker2 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            dagpenger = DagpengerForOpensearch(
                true,
                DagpengerRettighetstype.DAGPENGER_ARBEIDSSOKER_ORDINAER,
                tidspunkt1,
                2,
                null
            )
        )

        val bruker3 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            dagpenger = DagpengerForOpensearch(
                true,
                DagpengerRettighetstype.DAGPENGER_PERMITTERING_FISKEINDUSTRI,
                tidspunkt2,
                3,
                null
            )
        )

        val bruker4 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            dagpenger = DagpengerForOpensearch(
                true,
                DagpengerRettighetstype.DAGPENGER_PERMITTERING_ORDINAER,
                tidspunkt3,
                4,
                null
            )
        )


        val liste = listOf(bruker1, bruker2, bruker3, bruker4)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = getFiltervalgDefaults().copy(
            ytelseDagpenger = listOf(
                YtelseDagpenger.HAR_DAGPENGER_MED_PERMITTERING,
                YtelseDagpenger.HAR_DAGPENGER_ORDINAER,
                YtelseDagpenger.HAR_DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI
            )
        )

        val brukereMedAntall = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.DAGPENGER_ANTALL_RESTERENDE_DAGER,
            filtervalg,
            null,
            null
        )
        val brukereMedAntall2 = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.SYNKENDE,
            Sorteringsfelt.DAGPENGER_ANTALL_RESTERENDE_DAGER,
            filtervalg,
            null,
            null
        )

        val brukereMedAntallRettighet = opensearchService.hentBrukere(
            TEST_ENHET,
            Optional.empty(),
            Sorteringsrekkefolge.STIGENDE,
            Sorteringsfelt.DAGPENGER_RETTIGHETSTYPE,
            filtervalg,
            null,
            null
        )

        val brukereStigende = brukereMedAntall.brukere
        val brukereSynkende = brukereMedAntall2.brukere
        val brukereRettighetStigende = brukereMedAntallRettighet.brukere

        Assertions.assertThat(brukereStigende.size).isEqualTo(4)
        Assertions.assertThat(brukereStigende[0].fnr).isEqualTo(bruker2.fnr)
        Assertions.assertThat(brukereStigende[1].fnr).isEqualTo(bruker3.fnr)
        Assertions.assertThat(brukereStigende[3].fnr).isEqualTo(bruker1.fnr)

        Assertions.assertThat(brukereSynkende[0].fnr).isEqualTo(bruker4.fnr)
        Assertions.assertThat(brukereSynkende[1].fnr).isEqualTo(bruker3.fnr)
        Assertions.assertThat(brukereSynkende[3].fnr).isEqualTo(bruker1.fnr)

        Assertions.assertThat(brukereRettighetStigende[0].fnr).isEqualTo(bruker2.fnr)
        Assertions.assertThat(brukereRettighetStigende[1].fnr).isEqualTo(bruker3.fnr)

        Assertions.assertThat(brukereRettighetStigende[0].fnr).isEqualTo(bruker2.fnr)
        Assertions.assertThat(brukereRettighetStigende[1].fnr).isEqualTo(bruker3.fnr)
        Assertions.assertThat(listOf(brukereRettighetStigende[2].fnr, brukereRettighetStigende[3].fnr))
            .containsExactlyInAnyOrder(bruker1.fnr, bruker4.fnr)
    }

    @Test
    fun skal_sortere_brukere_pa_aap_rettighetstype() {
        val bistandsbehovBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            aap_kelvin = true,
            aap_kelvin_rettighetstype = AapRettighetstype.BISTANDSBEHOV,
            aap_kelvin_tom_vedtaksdato = LocalDate.now()
        )

        val studentBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            aap_kelvin = true,
            aap_kelvin_rettighetstype = AapRettighetstype.STUDENT,
            aap_kelvin_tom_vedtaksdato = LocalDate.now()
        )

        val sykepengeerstatningBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            aap_kelvin = true,
            aap_kelvin_rettighetstype = AapRettighetstype.SYKEPENGEERSTATNING,
            aap_kelvin_tom_vedtaksdato = LocalDate.now()
        )

        val nullBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            aap_kelvin = false,
        )


        val liste = listOf(sykepengeerstatningBruker, bistandsbehovBruker, studentBruker, nullBruker)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            ytelseAapKelvin = listOf(YtelseAapKelvin.HAR_AAP)
        )

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

        Assertions.assertThat(brukereStigende.size).isEqualTo(3)
        Assertions.assertThat(brukereStigende[0].fnr).isEqualTo(bistandsbehovBruker.fnr)
        Assertions.assertThat(brukereStigende[1].fnr).isEqualTo(studentBruker.fnr)

        Assertions.assertThat(brukereSynkende[0].fnr).isEqualTo(sykepengeerstatningBruker.fnr)
        Assertions.assertThat(brukereSynkende[2].fnr).isEqualTo(bistandsbehovBruker.fnr)
    }


    @Test
    fun skal_sortere_brukere_pa_tiltakspenger_rettighet() {
        val bruker1 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            tiltakspenger = true,
            tiltakspenger_rettighet = TiltakspengerRettighet.TILTAKSPENGER,
            tiltakspenger_vedtaksdato_tom = LocalDate.now()
        )

        val bruker2 = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            tiltakspenger = true,
            tiltakspenger_rettighet = TiltakspengerRettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            tiltakspenger_vedtaksdato_tom = LocalDate.now()
        )

        val nullBruker = PortefoljebrukerOpensearchModell(
            fnr = randomFnr().toString(),
            aktoer_id = randomAktorId().toString(),
            oppfolging = true,
            enhet_id = TEST_ENHET,
            tiltakspenger = false,
        )


        val liste = listOf(bruker1, bruker2, nullBruker)
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val filtervalg = getFiltervalgDefaults().copy(
            ferdigfilterListe = emptyList(),
            ytelseTiltakspenger = listOf(YtelseTiltakspenger.HAR_TILTAKSPENGER)
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

        Assertions.assertThat(brukereStigende.size).isEqualTo(2)
        Assertions.assertThat(brukereStigende[0].fnr).isEqualTo(bruker1.fnr)
        Assertions.assertThat(brukereStigende[1].fnr).isEqualTo(bruker2.fnr)

        Assertions.assertThat(brukereSynkende[0].fnr).isEqualTo(bruker2.fnr)
    }


    private fun skrivBrukereTilTestindeks(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(BRUKERINDEKS_ALIAS, listOf(*brukere.toTypedArray()))
    }
}
