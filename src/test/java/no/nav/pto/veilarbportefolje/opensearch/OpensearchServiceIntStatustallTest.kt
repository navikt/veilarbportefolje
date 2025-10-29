package no.nav.pto.veilarbportefolje.opensearch

import no.nav.common.auth.context.AuthContext
import no.nav.common.auth.context.AuthContextHolder
import no.nav.common.auth.context.UserRole
import no.nav.common.types.identer.EnhetId
import no.nav.common.utils.fn.UnsafeSupplier
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient
import no.nav.pto.veilarbportefolje.config.FeatureToggle
import no.nav.pto.veilarbportefolje.domene.*
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.opensearch.domene.StatustallResponse.StatustallAggregationKey
import no.nav.pto.veilarbportefolje.util.DateUtils
import no.nav.pto.veilarbportefolje.util.EndToEndTest
import no.nav.pto.veilarbportefolje.util.OpensearchTestClient
import no.nav.pto.veilarbportefolje.util.TestDataUtils.generateJWT
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomNavKontor
import no.nav.pto.veilarbportefolje.util.TestDataUtils.randomVeilederId
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.ThrowingSupplier
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import java.lang.reflect.Field
import java.time.LocalDateTime
import java.util.*

class OpensearchServiceIntStatustallTest @Autowired constructor(
    private val opensearchService: OpensearchService,
    private val authContextHolder: AuthContextHolder
) : EndToEndTest(){
    @Autowired
    private lateinit var veilarbVeilederClient: VeilarbVeilederClient
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

        val veilederePaEnhet = listOf(TEST_VEILEDER_0, TEST_VEILEDER_1, TEST_VEILEDER_2, TEST_VEILEDER_3)
        Mockito.doReturn(veilederePaEnhet).`when`(veilarbVeilederClient)
            .hentVeilederePaaEnhet(EnhetId.of(TEST_ENHET))

        Mockito.doReturn(false).`when`(defaultUnleash).isEnabled(FeatureToggle.BRUK_FILTER_FOR_BRUKERINNSYN_TILGANGER)
    }

    @Test
    fun skal_hente_riktige_statustall_for_veileder_naar_feature_toggle_er_av() {
        listOf(
            TEST_VEILEDER_0,
            TEST_VEILEDER_1,
            TEST_VEILEDER_2,
            TEST_VEILEDER_3
        )

        val kode6Bruker = genererRandomBruker(
            TEST_ENHET,
            null,
            "6",
            false
        )
        val kode6BrukerMedTilordnetVeileder = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0,
            "6",
            false
        )
        val kode6BrukerSomVenterPaSvarFraNav = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_1,
            "6",
            false
        ).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val kode7Bruker = genererRandomBruker(
            TEST_ENHET,
            null,
            "7",
            false
        )
        val kode7BrukerMedTilordnetVeileder = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0,
            "7",
            false
        )
        val kode7BrukerSomVenterPaSvarFraNav = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_1,
            "7",
            false
        ).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val egenAnsattBruker = genererRandomBruker(
            TEST_ENHET,
            null,
            null,
            true
        )
        val egenAnsattBrukerMedTilordnetVeileder = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0,
            null,
            true
        )
        val egenAnsattBrukerSomVenterPaSvarFraNav = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_1,
            null,
            true
        ).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val egenAnsattOgKode7Bruker = genererRandomBruker(
            TEST_ENHET,
            null,
            "7",
            true
        )
        val egenAnsattOgKode7BrukerMedTilordnetVeileder = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0,
            "7",
            true
        )
        val egenAnsattOgKode7BrukerSomVenterPaSvarFraNav =
            genererRandomBruker(
                TEST_ENHET,
                TEST_VEILEDER_1,
                "7",
                true
            ).setVenterpasvarfranav(
                DateUtils.toIsoUTC(LocalDateTime.now())
            )

        val tilfeldigFordeltBruker = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0,
            null,
            false
        )
        val tilfeldigUfordeltBruker = genererRandomBruker(
            TEST_ENHET,
            null,
            null,
            false
        )
        val tilfeldigBrukerSomVenterPaSvarFraNav = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0,
            null,
            false
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
            tilfeldigFordeltBruker,
            tilfeldigUfordeltBruker,
            tilfeldigBrukerSomVenterPaSvarFraNav
        )

        skrivBrukereTilTestindeks(brukere)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val responsNyttEndepunkt: Statustall = authContextHolder.withContext(
            AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDER_0)),
            UnsafeSupplier {
                opensearchService.hentStatustallForVeilederPortefolje(
                    TEST_VEILEDER_0,
                    TEST_ENHET
                )
            }
        )

        Assertions.assertThat(responsNyttEndepunkt.totalt).isEqualTo(6)
        Assertions.assertThat(responsNyttEndepunkt.venterPaSvarFraNAV).isEqualTo(1)
    }

    @Test
    fun skal_hente_riktige_statustall_for_enhet_naar_feature_toggle_er_av() {
        listOf(
            TEST_VEILEDER_0,
            TEST_VEILEDER_1,
            TEST_VEILEDER_2,
            TEST_VEILEDER_3
        )

        val kode6Bruker = genererRandomBruker(
            TEST_ENHET,
            null,
            "6",
            false
        )
        val kode6BrukerMedTilordnetVeileder = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0,
            "6",
            false
        )
        val kode6BrukerSomVenterPaSvarFraNav = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_1,
            "6",
            false
        ).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val kode7Bruker = genererRandomBruker(
            TEST_ENHET,
            null,
            "7",
            false
        )
        val kode7BrukerMedTilordnetVeileder = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0,
            "7",
            false
        )
        val kode7BrukerSomVenterPaSvarFraNav = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_1,
            "7",
            false
        ).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val egenAnsattBruker = genererRandomBruker(
            TEST_ENHET,
            null,
            null,
            true
        )
        val egenAnsattBrukerMedTilordnetVeileder = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0,
            null,
            true
        )
        val egenAnsattBrukerSomVenterPaSvarFraNav = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_1,
            null,
            true
        ).setVenterpasvarfranav(
            DateUtils.toIsoUTC(LocalDateTime.now())
        )

        val egenAnsattOgKode7Bruker = genererRandomBruker(
            TEST_ENHET,
            null,
            "7",
            true
        )
        val egenAnsattOgKode7BrukerMedTilordnetVeileder = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0,
            "7",
            true
        )
        val egenAnsattOgKode7BrukerSomVenterPaSvarFraNav =
            genererRandomBruker(
                TEST_ENHET,
                TEST_VEILEDER_1,
                "7",
                true
            ).setVenterpasvarfranav(
                DateUtils.toIsoUTC(LocalDateTime.now())
            )

        val tilfeldigFordeltBruker = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0,
            null,
            false
        )
        val tilfeldigUfordeltBruker = genererRandomBruker(
            TEST_ENHET,
            null,
            null,
            false
        )
        val tilfeldigBrukerSomVenterPaSvarFraNav = genererRandomBruker(
            TEST_ENHET,
            TEST_VEILEDER_0,
            null,
            false
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
            tilfeldigFordeltBruker,
            tilfeldigUfordeltBruker,
            tilfeldigBrukerSomVenterPaSvarFraNav
        )

        skrivBrukereTilTestindeks(brukere)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == brukere.size }

        val responsMedBrukerinnsyn: Statustall = authContextHolder.withContext(
            AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDER_0)),
            UnsafeSupplier {
                opensearchService.hentStatusTallForEnhetPortefolje(
                    TEST_ENHET,
                    BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ
                )
            }
        )
        val responsUtenBrukerinnsyn: Statustall = authContextHolder.withContext(
            AuthContext(UserRole.INTERN, generateJWT(TEST_VEILEDER_0)),
            UnsafeSupplier {
                opensearchService.hentStatusTallForEnhetPortefolje(
                    TEST_ENHET,
                    BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ
                )
            }
        )

        Assertions.assertThat(responsMedBrukerinnsyn.totalt + responsUtenBrukerinnsyn.totalt).isEqualTo(15)
        Assertions.assertThat(responsMedBrukerinnsyn.venterPaSvarFraNAV + responsUtenBrukerinnsyn.venterPaSvarFraNAV)
            .isEqualTo(5)
    }

    @Test
    fun skal_hente_riktige_statustall_for_enhet() {
        val brukerUtenVeileder = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().get())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)

        val brukerMedVeileder = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().get())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)

        val liste = listOf(brukerMedVeileder, brukerUtenVeileder)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        Mockito.`when`<List<String>>(veilarbVeilederClient.hentVeilederePaaEnhet(ArgumentMatchers.any()))
            .thenReturn(listOf(TEST_VEILEDER_0))

        val statustallForBrukereSomVeilederHarInnsynsrettPaa = opensearchService.hentStatusTallForEnhetPortefolje(
            TEST_ENHET,
            BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ
        )
        val statustallForBrukereSomVeilederIkkeHarInnsynsrettPaa = opensearchService.hentStatusTallForEnhetPortefolje(
            TEST_ENHET,
            BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ
        )

        Assertions.assertThat(statustallForBrukereSomVeilederHarInnsynsrettPaa.ufordelteBrukere).isEqualTo(1)
        Assertions.assertThat(statustallForBrukereSomVeilederIkkeHarInnsynsrettPaa.ufordelteBrukere).isZero()
    }

    @Test
    fun skal_mappe_statustall_for_samtlige_aggregation_keys() {
        val brukerUtenVeileder = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().get())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)

        val brukerMedVeileder = PortefoljebrukerOpensearchModell()
            .setFnr(randomFnr().toString())
            .setAktoer_id(randomAktorId().get())
            .setOppfolging(true)
            .setEnhet_id(TEST_ENHET)
            .setVeileder_id(TEST_VEILEDER_0)

        val liste = listOf(brukerMedVeileder, brukerUtenVeileder)

        skrivBrukereTilTestindeks(liste)

        OpensearchTestClient.pollOpensearchUntil { opensearchTestClient.countDocuments() == liste.size }

        val veilederStatustall = opensearchService.hentStatustallForVeilederPortefolje(
            TEST_VEILEDER_0,
            TEST_ENHET
        )
        val enhetStatustallForBrukereSomVeilederHarInnsynsrettPaa = opensearchService.hentStatusTallForEnhetPortefolje(
            TEST_ENHET,
            BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_HAR_INNSYNSRETT_PÅ
        )
        val enhetStatustallForBrukereSomVeilederIkkeHarInnsynsrettPaa =
            opensearchService.hentStatusTallForEnhetPortefolje(
                TEST_ENHET,
                BrukerinnsynTilgangFilterType.BRUKERE_SOM_VEILEDER_IKKE_HAR_INNSYNSRETT_PÅ
            )

        Arrays.stream(StatustallAggregationKey.entries.toTypedArray())
            .forEach { key: StatustallAggregationKey ->
                org.junit.jupiter.api.Assertions.assertDoesNotThrow<Field> {
                    veilederStatustall.javaClass.getDeclaredField(
                        key.key
                    )
                }
                org.junit.jupiter.api.Assertions.assertDoesNotThrow<Field> {
                    enhetStatustallForBrukereSomVeilederHarInnsynsrettPaa.javaClass.getDeclaredField(
                        key.key
                    )
                }
                org.junit.jupiter.api.Assertions.assertDoesNotThrow(ThrowingSupplier {
                    enhetStatustallForBrukereSomVeilederIkkeHarInnsynsrettPaa.javaClass.getDeclaredField(
                        key.key
                    )
                })
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
    private fun skrivBrukereTilTestindeks(brukere: List<PortefoljebrukerOpensearchModell>) {
        opensearchIndexer.skrivBulkTilIndeks(indexName.value, listOf(*brukere.toTypedArray()))
    }
}
