package no.nav.pto.veilarbportefolje.opensearch

import no.nav.pto.veilarbportefolje.aap.domene.AapRettighetstype
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.filtervalg.*
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
        opensearchIndexer.skrivBulkTilIndeks(indexName.value, listOf(*brukere.toTypedArray()))
    }
}
