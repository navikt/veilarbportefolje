package no.nav.pto.veilarbportefolje.opensearch

import no.nav.common.auth.context.AuthContext
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.UserRole
import no.nav.common.types.identer.EnhetId
import no.nav.poao_tilgang.client.Decision.Deny
import no.nav.poao_tilgang.client.Decision.Permit
import no.nav.pto.veilarbportefolje.auth.PoaoTilgangWrapper
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient
import no.nav.pto.veilarbportefolje.config.FeatureToggle
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.domene.BrukereMedAntall
import no.nav.pto.veilarbportefolje.domene.filtervalg.Filtervalg
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModell
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriVerdi
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.domene.Adressebeskyttelse
import no.nav.pto.veilarbportefolje.util.DateUtils
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient
import no.nav.pto.veilarbportefolje.util.TestDataUtils.generateJWT
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class OpensearchServiceIntTilgangskontrollGenerellTest @Autowired constructor(
    private val opensearchService: OpensearchService,
    private val poaoTilgangWrapper: PoaoTilgangWrapper,
    private val authContextHolder: AuthContextHolder
) : EndToEndTest() {
    @Autowired
    private lateinit var veilarbVeilederClient: VeilarbVeilederClient
    private lateinit var veilederePaEnhet: List<String>
    private lateinit var TEST_ENHET: String
    private lateinit var TEST_VEILEDER_0: String
    private lateinit var TEST_VEILEDER_1: String
    private lateinit var TEST_VEILEDER_2: String
    private lateinit var TEST_VEILEDER_3: String

    @BeforeEach
    fun setup() {
        TEST_ENHET = randomNavKontor().value
        TEST_VEILEDER_0 = randomVeilederId().value
        TEST_VEILEDER_1 = randomVeilederId().value
        TEST_VEILEDER_2 = randomVeilederId().value
        TEST_VEILEDER_3 = randomVeilederId().value


        veilederePaEnhet = listOf(
            TEST_VEILEDER_0, TEST_VEILEDER_1, TEST_VEILEDER_2, TEST_VEILEDER_3
        )

        Mockito.doReturn(veilederePaEnhet).`when`(veilarbVeilederClient)
            .hentVeilederePaaEnhet(EnhetId.of(TEST_ENHET))

        Mockito.doReturn(true).`when`(defaultUnleash).isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER)

        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode6()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode7()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilEgenAnsatt()
    }
    @AfterEach
    fun cleanup() {
        Mockito.reset(veilarbVeilederClient, defaultUnleash, poaoTilgangWrapper)
    }

    @Test
    fun skal_hente_riktige_statustall_for_enhet_nar_veileder_ikke_har_spesiell_brukerinnsyn_tilgang() {

        val kode6Bruker = createKode6Bruker(TEST_ENHET, null)

        val kode6BrukerMedTilordnetVeileder = createKode6Bruker(TEST_ENHET, TEST_VEILEDER_0)

        val kode6BrukerSomVenterPaSvarFraNav = createKode6Bruker(TEST_ENHET, TEST_VEILEDER_0).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val kode7Bruker = createKode7Bruker(TEST_ENHET, null)

        val kode7BrukerMedTilordnetVeileder = createKode7Bruker(TEST_ENHET, TEST_VEILEDER_1)

        val kode7BrukerSomVenterPaSvarFraNav = createKode7Bruker(TEST_ENHET, TEST_VEILEDER_1).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val egenAnsattBruker = createEgenAnsattBruker(TEST_ENHET, null)

        val egenAnsattBrukerMedTilordnetVeileder = createEgenAnsattBruker(TEST_ENHET, TEST_VEILEDER_2)

        val egenAnsattBrukerSomVenterPaSvarFraNav =
            createEgenAnsattBruker(TEST_ENHET, TEST_VEILEDER_2).setVenterpasvarfranav(
                DateUtils.toIsoUTC(LocalDateTime.now())
            )

        val egenAnsattOgKode7Bruker = createEgenAnsattOgKode7Bruker(TEST_ENHET, null)

        val egenAnsattOgKode7BrukerMedTilordnetVeileder = createEgenAnsattOgKode7Bruker(TEST_ENHET, TEST_VEILEDER_3)

        val egenAnsattOgKode7BrukerSomVenterPaSvarFraNav =
            createEgenAnsattOgKode7Bruker(TEST_ENHET, TEST_VEILEDER_3).setVenterpasvarfranav(
                DateUtils.toIsoUTC(LocalDateTime.now())
            )

        val fordeltBrukerSomIkkeSkalInkluderes = createRegularBruker(TEST_ENHET, TEST_VEILEDER_3)

        val ufordeltBrukerSomIkkeSkalInkluderes = createRegularBruker(TEST_ENHET, null)

        val brukerSomVenterPaSvarFraNavSomIkkeSkalInkluderes = createRegularBruker(
            TEST_ENHET, null
        ).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val brukere = listOf(
            kode6Bruker,
            kode6BrukerMedTilordnetVeileder,
            kode6BrukerSomVenterPaSvarFraNav,
            kode7Bruker,
            kode7BrukerMedTilordnetVeileder,
            kode7BrukerSomVenterPaSvarFraNav,
            egenAnsattBruker,
            egenAnsattBrukerMedTilordnetVeileder,
            egenAnsattBrukerSomVenterPaSvarFraNav,
            egenAnsattOgKode7Bruker,
            egenAnsattOgKode7BrukerMedTilordnetVeileder,
            egenAnsattOgKode7BrukerSomVenterPaSvarFraNav,
            fordeltBrukerSomIkkeSkalInkluderes,
            ufordeltBrukerSomIkkeSkalInkluderes,
            brukerSomVenterPaSvarFraNavSomIkkeSkalInkluderes
        )

        skrivBrukereTilTestindeks(brukere)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val respons = opensearchService.hentStatusTallForEnhetPortefolje(
            TEST_ENHET, BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ
        )

        assertThat(respons.totalt).isEqualTo(12)
        assertThat(respons.ufordelteBrukere).isEqualTo(4)
        assertThat(respons.venterPaSvarFraNAV).isEqualTo(4)
    }

    @Test
    fun skal_hente_riktige_statustall_for_veileder_naar_veileder_har_brukerinnsyn_tilgang_til_kode_7_brukere() {

        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode6()
        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode7()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilEgenAnsatt()

        val kode6Bruker = createKode6Bruker(TEST_ENHET, null)

        val kode6BrukerMedTilordnetVeileder = createKode6Bruker(TEST_ENHET, TEST_VEILEDER_0)

        val kode6BrukerSomVenterPaSvarFraNav = createKode6Bruker(TEST_ENHET, TEST_VEILEDER_1).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val kode7Bruker = createKode7Bruker(TEST_ENHET, null)

        val kode7BrukerMedTilordnetVeileder = createKode7Bruker(TEST_ENHET, TEST_VEILEDER_0)
        val kode7BrukerSomVenterPaSvarFraNav = createKode7Bruker(TEST_ENHET, TEST_VEILEDER_1).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val egenAnsattBruker = createEgenAnsattBruker(TEST_ENHET, null)

        val egenAnsattBrukerMedTilordnetVeileder = createEgenAnsattBruker(TEST_ENHET, TEST_VEILEDER_0)

        val egenAnsattBrukerSomVenterPaSvarFraNav =
            createEgenAnsattBruker(TEST_ENHET, TEST_VEILEDER_1).setVenterpasvarfranav(
                DateUtils.toIsoUTC(LocalDateTime.now())
            )

        val egenAnsattOgKode7Bruker = createEgenAnsattOgKode7Bruker(TEST_ENHET, null)

        val egenAnsattOgKode7BrukerMedTilordnetVeileder = createEgenAnsattOgKode7Bruker(TEST_ENHET, TEST_VEILEDER_0)

        val egenAnsattOgKode7BrukerSomVenterPaSvarFraNav =
            createEgenAnsattOgKode7Bruker(TEST_ENHET, TEST_VEILEDER_1).setVenterpasvarfranav(
                DateUtils.toIsoUTC(LocalDateTime.now())
            )

        val tilfeldigFordeltBruker = createRegularBruker(TEST_ENHET, TEST_VEILEDER_0)

        val tilfeldigUfordeltBruker = createRegularBruker(TEST_ENHET, null)

        val tilfeldigBrukerSomVenterPaSvarFraNav =
            createRegularBruker(TEST_ENHET, TEST_VEILEDER_0).setVenterpasvarfranav(
                DateUtils.toIsoUTC(LocalDateTime.now())
            )

        val brukere = listOf(
            kode6Bruker,
            kode6BrukerMedTilordnetVeileder,
            kode6BrukerSomVenterPaSvarFraNav,
            kode7Bruker,
            kode7BrukerMedTilordnetVeileder,
            kode7BrukerSomVenterPaSvarFraNav,
            egenAnsattBruker,
            egenAnsattBrukerMedTilordnetVeileder,
            egenAnsattBrukerSomVenterPaSvarFraNav,
            egenAnsattOgKode7Bruker,
            egenAnsattOgKode7BrukerMedTilordnetVeileder,
            egenAnsattOgKode7BrukerSomVenterPaSvarFraNav,
            tilfeldigFordeltBruker,
            tilfeldigUfordeltBruker,
            tilfeldigBrukerSomVenterPaSvarFraNav
        )

        skrivBrukereTilTestindeks(brukere)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val respons = opensearchService.hentStatusTallForEnhetPortefolje(
            TEST_ENHET, BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ
        )

        assertThat(respons.totalt).isEqualTo(6)
        assertThat(respons.venterPaSvarFraNAV).isEqualTo(2)
    }

    @Test
    fun skal_hente_riktige_statustall_for_enhet_naar_veileder_har_alle_brukerinnsyn_tilganger() {

        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode6()
        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode7()
        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilEgenAnsatt()

        val kode6Bruker = createKode6Bruker(TEST_ENHET, null)

        val kode6BrukerMedTilordnetVeileder = createKode6Bruker(TEST_ENHET, TEST_VEILEDER_0)

        val kode6BrukerSomVenterPaSvarFraNav = createKode6Bruker(TEST_ENHET, TEST_VEILEDER_1).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val kode7Bruker = createKode7Bruker(TEST_ENHET, null)

        val kode7BrukerMedTilordnetVeileder = createKode7Bruker(TEST_ENHET, TEST_VEILEDER_0)

        val kode7BrukerSomVenterPaSvarFraNav = createKode7Bruker(TEST_ENHET, TEST_VEILEDER_1).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val egenAnsattBruker = createEgenAnsattBruker(TEST_ENHET, null)

        val egenAnsattBrukerMedTilordnetVeileder = createEgenAnsattBruker(TEST_ENHET, TEST_VEILEDER_0)

        val egenAnsattBrukerSomVenterPaSvarFraNav =
            createEgenAnsattBruker(TEST_ENHET, TEST_VEILEDER_1).setVenterpasvarfranav(
                DateUtils.toIsoUTC(LocalDateTime.now())
            )

        val egenAnsattOgKode7Bruker = createEgenAnsattOgKode7Bruker(TEST_ENHET, null)

        val egenAnsattOgKode7BrukerMedTilordnetVeileder = createEgenAnsattOgKode7Bruker(
            TEST_ENHET, TEST_VEILEDER_0
        )

        val egenAnsattOgKode7BrukerSomVenterPaSvarFraNav =
            createEgenAnsattOgKode7Bruker(TEST_ENHET, TEST_VEILEDER_1).setVenterpasvarfranav(
                DateUtils.toIsoUTC(LocalDateTime.now())
            )

        val tilfeldigFordeltBruker = createRegularBruker(TEST_ENHET, TEST_VEILEDER_0)

        val tilfeldigUfordeltBruker = createRegularBruker(TEST_ENHET, null)

        val tilfeldigBrukerSomVenterPaSvarFraNav =
            createRegularBruker(TEST_ENHET, TEST_VEILEDER_0).setVenterpasvarfranav(
                DateUtils.toIsoUTC(LocalDateTime.now())
            )

        val brukere = listOf(
            kode6Bruker,
            kode6BrukerMedTilordnetVeileder,
            kode6BrukerSomVenterPaSvarFraNav,
            kode7Bruker,
            kode7BrukerMedTilordnetVeileder,
            kode7BrukerSomVenterPaSvarFraNav,
            egenAnsattBruker,
            egenAnsattBrukerMedTilordnetVeileder,
            egenAnsattBrukerSomVenterPaSvarFraNav,
            egenAnsattOgKode7Bruker,
            egenAnsattOgKode7BrukerMedTilordnetVeileder,
            egenAnsattOgKode7BrukerSomVenterPaSvarFraNav,
            tilfeldigFordeltBruker,
            tilfeldigUfordeltBruker,
            tilfeldigBrukerSomVenterPaSvarFraNav
        )

        skrivBrukereTilTestindeks(brukere)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }


        val responsUtenBrukerinnsyn: Statustall = authContextHolder.withContext<Statustall>(
            AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDER_0))
        ) {
            opensearchService.hentStatusTallForEnhetPortefolje(
                TEST_ENHET, BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ
            )
        }

        val responsMedBrukerinnsyn: Statustall = authContextHolder.withContext<Statustall>(
            AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDER_0))
        ) {
            opensearchService.hentStatusTallForEnhetPortefolje(
                TEST_ENHET, BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ
            )
        }

        assertThat(responsMedBrukerinnsyn.totalt).isEqualTo(15)
        assertThat(responsUtenBrukerinnsyn.totalt).isZero()
        assertThat(responsMedBrukerinnsyn.venterPaSvarFraNAV).isEqualTo(5)
        assertThat(responsMedBrukerinnsyn.ufordelteBrukere).isEqualTo(5)
    }

    @Test
    fun skal_kun_hente_brukere_som_innlogget_veileder_har_innsynsrett_pa_nar_man_henter_enhetens_portefolje() {

        val kode6Bruker0 = createKode6Bruker(TEST_ENHET, null)
        val kode6Bruker1 = createKode6Bruker(TEST_ENHET, null)
        val kode6Bruker2 = createKode6Bruker(TEST_ENHET, null)
        val kode6Bruker3 = createKode6Bruker(TEST_ENHET, null)

        val kode7Bruker0 = createKode7Bruker(TEST_ENHET, null)
        val kode7Bruker1 = createKode7Bruker(TEST_ENHET, null)
        val kode7Bruker2 = createKode7Bruker(TEST_ENHET, null)
        val kode7Bruker3 = createKode7Bruker(TEST_ENHET, null)

        val egenAnsattBruker0 = createEgenAnsattBruker(TEST_ENHET, null)
        val egenAnsattBruker1 = createEgenAnsattBruker(TEST_ENHET, null)
        val egenAnsattBruker2 = createEgenAnsattBruker(TEST_ENHET, null)
        val egenAnsattBruker3 = createEgenAnsattBruker(TEST_ENHET, null)

        val egenAnsattOgKode7Bruker0 = createEgenAnsattOgKode7Bruker(TEST_ENHET, null)
        val egenAnsattOgKode7Bruker1 = createEgenAnsattOgKode7Bruker(TEST_ENHET, null)
        val egenAnsattOgKode7Bruker2 = createEgenAnsattOgKode7Bruker(TEST_ENHET, null)
        val egenAnsattOgKode7Bruker3 = createEgenAnsattOgKode7Bruker(TEST_ENHET, null)

        val brukere = listOf(
            kode6Bruker0,
            kode6Bruker1,
            kode6Bruker2,
            kode6Bruker3,
            kode7Bruker0,
            kode7Bruker1,
            kode7Bruker2,
            kode7Bruker3,
            egenAnsattBruker0,
            egenAnsattBruker1,
            egenAnsattBruker2,
            egenAnsattBruker3,
            egenAnsattOgKode7Bruker0,
            egenAnsattOgKode7Bruker1,
            egenAnsattOgKode7Bruker2,
            egenAnsattOgKode7Bruker3
        )

        skrivBrukereTilTestindeks(brukere)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode6()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode7()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilEgenAnsatt()
        val brukereSomVeilederMedKode6TilgangHarInnsynsrettPa = loggInnVeilederOgHentEnhetPortefolje(
            opensearchService, TEST_VEILEDER_0, authContextHolder
        )

        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode6()
        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode7()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilEgenAnsatt()
        val brukereSomVeilederMedKode7TilgangHarInnsynsrettPa = loggInnVeilederOgHentEnhetPortefolje(
            opensearchService, TEST_VEILEDER_1, authContextHolder
        )

        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode6()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode7()
        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilEgenAnsatt()
        val brukereSomVeilederMedEgenAnsattTilgangHarInnsynsrettPa = loggInnVeilederOgHentEnhetPortefolje(
            opensearchService, TEST_VEILEDER_2, authContextHolder
        )

        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode6()
        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode7()
        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilEgenAnsatt()
        val brukereSomVeilederMedEgenAnsattOgKode7TilgangHarInnsynsrettPa = loggInnVeilederOgHentEnhetPortefolje(
            opensearchService, TEST_VEILEDER_3, authContextHolder
        )

        assertThat(brukereSomVeilederMedKode6TilgangHarInnsynsrettPa.antall).isEqualTo(4)
        assertThat(brukereSomVeilederMedKode7TilgangHarInnsynsrettPa.antall).isEqualTo(4)
        assertThat(brukereSomVeilederMedEgenAnsattTilgangHarInnsynsrettPa.antall).isEqualTo(4)
        assertThat(brukereSomVeilederMedEgenAnsattOgKode7TilgangHarInnsynsrettPa.antall).isEqualTo(12)

        assertThat(brukereSomVeilederMedKode6TilgangHarInnsynsrettPa.brukere).containsExactlyInAnyOrder(
            PortefoljebrukerFrontendModell.of(kode6Bruker0, true),
            PortefoljebrukerFrontendModell.of(kode6Bruker1, true),
            PortefoljebrukerFrontendModell.of(kode6Bruker2, true),
            PortefoljebrukerFrontendModell.of(kode6Bruker3, true)
        )
        assertThat(brukereSomVeilederMedKode7TilgangHarInnsynsrettPa.brukere).containsExactlyInAnyOrder(
            PortefoljebrukerFrontendModell.of(kode7Bruker0, true),
            PortefoljebrukerFrontendModell.of(kode7Bruker1, true),
            PortefoljebrukerFrontendModell.of(kode7Bruker2, true),
            PortefoljebrukerFrontendModell.of(kode7Bruker3, true)
        )
        assertThat(brukereSomVeilederMedEgenAnsattTilgangHarInnsynsrettPa.brukere).containsExactlyInAnyOrder(
            PortefoljebrukerFrontendModell.of(egenAnsattBruker0, true),
            PortefoljebrukerFrontendModell.of(egenAnsattBruker1, true),
            PortefoljebrukerFrontendModell.of(egenAnsattBruker2, true),
            PortefoljebrukerFrontendModell.of(egenAnsattBruker3, true)
        )
        assertThat(brukereSomVeilederMedEgenAnsattOgKode7TilgangHarInnsynsrettPa.brukere).containsExactlyInAnyOrder(
            PortefoljebrukerFrontendModell.of(kode7Bruker0, true),
            PortefoljebrukerFrontendModell.of(kode7Bruker1, true),
            PortefoljebrukerFrontendModell.of(kode7Bruker2, true),
            PortefoljebrukerFrontendModell.of(kode7Bruker3, true),
            PortefoljebrukerFrontendModell.of(egenAnsattBruker0, true),
            PortefoljebrukerFrontendModell.of(egenAnsattBruker1, true),
            PortefoljebrukerFrontendModell.of(egenAnsattBruker2, true),
            PortefoljebrukerFrontendModell.of(egenAnsattBruker3, true),
            PortefoljebrukerFrontendModell.of(egenAnsattOgKode7Bruker0, true),
            PortefoljebrukerFrontendModell.of(egenAnsattOgKode7Bruker1, true),
            PortefoljebrukerFrontendModell.of(egenAnsattOgKode7Bruker2, true),
            PortefoljebrukerFrontendModell.of(egenAnsattOgKode7Bruker3, true)
        )
    }

    @Test
    fun skal_kun_hente_brukere_som_innlogget_veileder_har_innsynsrett_pa_nar_man_henter_veileders_portefolje() {

        val kode6BrukerMedVeileder0Tilordnet = createKode6Bruker(TEST_ENHET, TEST_VEILEDER_0)
        val kode7BrukerMedVeileder0Tilordnet = createKode7Bruker(TEST_ENHET, TEST_VEILEDER_0)
        val egenAnsattBrukerMedVeileder0Tilordnet = createEgenAnsattBruker(TEST_ENHET, TEST_VEILEDER_0)
        val egenAnsattOgKode7BrukerMedVeileder0Tilordnet = createEgenAnsattOgKode7Bruker(TEST_ENHET, TEST_VEILEDER_0)
        val kode6BrukerMedVeileder1Tilordnet = createKode6Bruker(TEST_ENHET, TEST_VEILEDER_1)
        val kode7BrukerMedVeileder1Tilordnet = createKode7Bruker(TEST_ENHET, TEST_VEILEDER_1)
        val egenAnsattBrukerMedVeileder1Tilordnet = createEgenAnsattBruker(TEST_ENHET, TEST_VEILEDER_1)
        val egenAnsattOgKode7BrukerMedVeileder1Tilordnet = createEgenAnsattOgKode7Bruker(TEST_ENHET, TEST_VEILEDER_1)
        val kode6BrukerMedVeileder2Tilordnet = createKode6Bruker(TEST_ENHET, TEST_VEILEDER_2)
        val kode7BrukerMedVeileder2Tilordnet = createKode7Bruker(TEST_ENHET, TEST_VEILEDER_2)
        val egenAnsattBrukerMedVeileder2Tilordnet = createEgenAnsattBruker(TEST_ENHET, TEST_VEILEDER_2)
        val egenAnsattOgKode7BrukerMedVeileder2Tilordnet = createEgenAnsattOgKode7Bruker(TEST_ENHET, TEST_VEILEDER_2)
        val kode6BrukerMedVeileder3Tilordnet = createKode6Bruker(TEST_ENHET, TEST_VEILEDER_3)
        val kode7BrukerMedVeileder3Tilordnet = createKode7Bruker(TEST_ENHET, TEST_VEILEDER_3)
        val egenAnsattBrukerMedVeileder3Tilordnet = createEgenAnsattBruker(TEST_ENHET, TEST_VEILEDER_3)
        val egenAnsattOgKode7BrukerMedVeileder3Tilordnet = createEgenAnsattOgKode7Bruker(TEST_ENHET, TEST_VEILEDER_3)

        val brukere = listOf(
            kode6BrukerMedVeileder0Tilordnet,
            kode7BrukerMedVeileder0Tilordnet,
            egenAnsattBrukerMedVeileder0Tilordnet,
            egenAnsattOgKode7BrukerMedVeileder0Tilordnet,
            kode6BrukerMedVeileder1Tilordnet,
            kode7BrukerMedVeileder1Tilordnet,
            egenAnsattBrukerMedVeileder1Tilordnet,
            egenAnsattOgKode7BrukerMedVeileder1Tilordnet,
            kode6BrukerMedVeileder2Tilordnet,
            kode7BrukerMedVeileder2Tilordnet,
            egenAnsattBrukerMedVeileder2Tilordnet,
            egenAnsattOgKode7BrukerMedVeileder2Tilordnet,
            kode6BrukerMedVeileder3Tilordnet,
            kode7BrukerMedVeileder3Tilordnet,
            egenAnsattBrukerMedVeileder3Tilordnet,
            egenAnsattOgKode7BrukerMedVeileder3Tilordnet
        )

        skrivBrukereTilTestindeks(brukere)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode6()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode7()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilEgenAnsatt()
        val brukereSomVeilederMedKode6TilgangHarInnsynsrettPa = loggInnVeilederOgHentVeilederPortefolje(
            opensearchService, TEST_VEILEDER_0, authContextHolder
        )

        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode6()
        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode7()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilEgenAnsatt()
        val brukereSomVeilederMedKode7TilgangHarInnsynsrettPa = loggInnVeilederOgHentVeilederPortefolje(
            opensearchService, TEST_VEILEDER_1, authContextHolder
        )

        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode6()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode7()
        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilEgenAnsatt()
        val brukereSomVeilederMedEgenAnsattTilgangHarInnsynsrettPa = loggInnVeilederOgHentVeilederPortefolje(
            opensearchService, TEST_VEILEDER_2, authContextHolder
        )

        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode6()
        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode7()
        Mockito.doReturn(Permit).`when`(poaoTilgangWrapper).harVeilederTilgangTilEgenAnsatt()
        val brukereSomVeilederMedEgenAnsattOgKode7TilgangHarInnsynsrettPa = loggInnVeilederOgHentVeilederPortefolje(
            opensearchService, TEST_VEILEDER_3, authContextHolder
        )

        assertThat(brukereSomVeilederMedKode6TilgangHarInnsynsrettPa.antall).isEqualTo(1)
        assertThat(brukereSomVeilederMedKode7TilgangHarInnsynsrettPa.antall).isEqualTo(1)
        assertThat(brukereSomVeilederMedEgenAnsattTilgangHarInnsynsrettPa.antall).isEqualTo(1)
        assertThat(brukereSomVeilederMedEgenAnsattOgKode7TilgangHarInnsynsrettPa.antall).isEqualTo(3)

        assertThat(brukereSomVeilederMedKode6TilgangHarInnsynsrettPa.brukere).containsExactlyInAnyOrder(
            PortefoljebrukerFrontendModell.of(kode6BrukerMedVeileder0Tilordnet, false)
        )
        assertThat(brukereSomVeilederMedKode7TilgangHarInnsynsrettPa.brukere).containsExactlyInAnyOrder(
            PortefoljebrukerFrontendModell.of(kode7BrukerMedVeileder1Tilordnet, false)
        )
        assertThat(brukereSomVeilederMedEgenAnsattTilgangHarInnsynsrettPa.brukere).containsExactlyInAnyOrder(
            PortefoljebrukerFrontendModell.of(egenAnsattBrukerMedVeileder2Tilordnet, false)
        )
        assertThat(brukereSomVeilederMedEgenAnsattOgKode7TilgangHarInnsynsrettPa.brukere).containsExactlyInAnyOrder(
            PortefoljebrukerFrontendModell.of(kode7BrukerMedVeileder3Tilordnet, false),
            PortefoljebrukerFrontendModell.of(egenAnsattBrukerMedVeileder3Tilordnet, false),
            PortefoljebrukerFrontendModell.of(egenAnsattOgKode7BrukerMedVeileder3Tilordnet, false)
        )
    }

    @Test
    fun skal_hente_riktige_statustall_for_veileder() {

        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode6()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilKode7()
        Mockito.doReturn(Deny("", "")).`when`(poaoTilgangWrapper).harVeilederTilgangTilEgenAnsatt()

        val testBruker1 = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().toString()).setFnr(randomFnr().toString())
            .setOppfolging(true).setEnhet_id(TEST_ENHET).setVeileder_id(TEST_VEILEDER_0).setHuskelapp(
                HuskelappForBruker(
                    LocalDate.now(),
                    "test huskelapp",
                    LocalDate.now(),
                    TEST_VEILEDER_0,
                    UUID.randomUUID().toString(),
                    TEST_ENHET
                )
            ).setFargekategori(FargekategoriVerdi.FARGEKATEGORI_A.name).setFargekategori_enhetId(TEST_ENHET)

        val testBruker2 = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().toString()).setFnr(randomFnr().toString())
            .setOppfolging(true).setEnhet_id(TEST_ENHET).setVeileder_id(TEST_VEILEDER_0)
            .setFormidlingsgruppekode("IARBS").setKvalifiseringsgruppekode("BATT").setAktiviteter(setOf("egen"))
            .setNy_for_veileder(true).setTrenger_vurdering(true)
            .setVenterpasvarfranav("2018-05-09T22:00:00Z").setNyesteutlopteaktivitet("2018-05-09T22:00:00Z")
            .setHuskelapp(null).setFargekategori(FargekategoriVerdi.FARGEKATEGORI_B.name)
            .setFargekategori_enhetId(TEST_ENHET)

        val inaktivBruker = PortefoljebrukerOpensearchModell()
            .setAktoer_id(randomAktorId().toString()).setFnr(randomFnr().toString())
            .setOppfolging(true).setEnhet_id(TEST_ENHET).setVeileder_id(TEST_VEILEDER_0)
            .setFormidlingsgruppekode("ISERV")

        val kode6BrukerSomVeilederIkkeHarInnsynsrettPa = genererRandomBruker(
            TEST_ENHET, TEST_VEILEDER_0, Adressebeskyttelse.STRENGT_FORTROLIG.diskresjonskode, false
        ).setVenterpasvarfranav(DateUtils.toIsoUTC(LocalDateTime.now()))
            .setFargekategori(FargekategoriVerdi.FARGEKATEGORI_A.name).setFargekategori_enhetId(TEST_ENHET)
        val kode7BrukerSomVeilederIkkeHarInnsynsrettPa = genererRandomBruker(
            TEST_ENHET, TEST_VEILEDER_0, Adressebeskyttelse.FORTROLIG.diskresjonskode, false
        ).setVenterpasvarfranav(DateUtils.toIsoUTC(LocalDateTime.now()))
            .setFargekategori(FargekategoriVerdi.FARGEKATEGORI_B.name).setFargekategori_enhetId(TEST_ENHET)
        val egenAnsattBrukerSomVeilederIkkeHarInnsynsrettPa = genererRandomBruker(
            TEST_ENHET, TEST_VEILEDER_0, null, true
        ).setVenterpasvarfranav(DateUtils.toIsoUTC(LocalDateTime.now()))

        val liste = listOf(
            testBruker1,
            testBruker2,
            inaktivBruker,
            kode6BrukerSomVeilederIkkeHarInnsynsrettPa,
            kode7BrukerSomVeilederIkkeHarInnsynsrettPa,
            egenAnsattBrukerSomVeilederIkkeHarInnsynsrettPa
        )
        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val statustall = opensearchService.hentStatustallForVeilederPortefolje(
            TEST_VEILEDER_0, TEST_ENHET
        )
        assertThat(statustall.erSykmeldtMedArbeidsgiver).isZero()
        assertThat(statustall.iavtaltAktivitet).isEqualTo(1)
        assertThat(statustall.ikkeIavtaltAktivitet).isEqualTo(2)
        assertThat(statustall.inaktiveBrukere).isEqualTo(1)
        assertThat(statustall.nyeBrukereForVeileder).isEqualTo(1)
        assertThat(statustall.venterPaSvarFraNAV).isEqualTo(1)
        assertThat(statustall.utlopteAktiviteter).isEqualTo(1)
        assertThat(statustall.mineHuskelapper).isEqualTo(1)
        assertThat(statustall.fargekategoriA).isEqualTo(1)
        assertThat(statustall.fargekategoriB).isEqualTo(1)
        assertThat(statustall.fargekategoriC).isZero()
        assertThat(statustall.fargekategoriD).isZero()
        assertThat(statustall.fargekategoriE).isZero()
        assertThat(statustall.fargekategoriF).isZero()
        assertThat(statustall.fargekategoriIngenKategori).isEqualTo(1)
        assertThat(statustall.tiltakshendelser).isZero()
    }

    private fun skrivBrukereTilTestindeks(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(indexName.value, listOf(*brukere.toTypedArray()))
    }

    private fun loggInnVeilederOgHentVeilederPortefolje(
        opensearchService: OpensearchService, veilederId: String, contextHolder: AuthContextHolder
    ): BrukereMedAntall {
        return contextHolder.withContext<BrukereMedAntall>(
            AuthContext(UserRole.INTERN, generateJWT(veilederId))
        ) {
            opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.of(veilederId),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                Filtervalg(),
                null,
                null
            )
        }
    }

    private fun loggInnVeilederOgHentEnhetPortefolje(
        opensearchService: OpensearchService, veilederId: String, contextHolder: AuthContextHolder
    ): BrukereMedAntall {
        return contextHolder.withContext<BrukereMedAntall>(
            AuthContext(UserRole.INTERN, generateJWT(veilederId))
        ) {
            opensearchService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                Sorteringsrekkefolge.IKKE_SATT,
                Sorteringsfelt.IKKE_SATT,
                Filtervalg(),
                null,
                null
            )
        }
    }

    private fun genererRandomBruker(
        enhet: String, veilederId: String?, diskresjonskode: String?, egenAnsatt: Boolean
    ): PortefoljebrukerOpensearchModell {
        val bruker =
            PortefoljebrukerOpensearchModell().setAktoer_id(randomAktorId().toString()).setFnr(randomFnr().get()).setOppfolging(true)
                .setEnhet_id(enhet)

        if (veilederId != null) {
            bruker.setVeileder_id(veilederId)
        }

        if (diskresjonskode != null) {
            bruker.setDiskresjonskode(diskresjonskode)
        }

        if (egenAnsatt) {
            bruker.setEgen_ansatt(true)
        }

        return bruker
    }

    private fun createKode6Bruker(enhet: String, veileder: String? = null) =
        genererRandomBruker(enhet, veileder, "6", false)

    private fun createKode7Bruker(enhet: String, veileder: String? = null) =
        genererRandomBruker(enhet, veileder, "7", false)

    private fun createEgenAnsattBruker(enhet: String, veileder: String? = null) =
        genererRandomBruker(enhet, veileder, null, true)

    private fun createEgenAnsattOgKode7Bruker(enhet: String, veileder: String? = null) =
        genererRandomBruker(enhet, veileder, "7", true)

    private fun createRegularBruker(enhet: String, veileder: String? = null) =
        genererRandomBruker(enhet, veileder, null, false)

}
