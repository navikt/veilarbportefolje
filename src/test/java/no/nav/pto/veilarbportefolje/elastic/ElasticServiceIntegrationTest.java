package no.nav.pto.veilarbportefolje.elastic;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.abac.Pep;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.utils.Pair;
import no.nav.pto.veilarbportefolje.arbeidsliste.Arbeidsliste;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.cv.IntegrationTest;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.*;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.RequestOptions;
import org.jetbrains.annotations.NotNull;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg;
import org.junit.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static no.nav.pto.veilarbportefolje.domene.Brukerstatus.*;
import static no.nav.pto.veilarbportefolje.elastic.ElasticUtils.createIndexName;
import static no.nav.pto.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.pto.veilarbportefolje.util.CollectionUtils.*;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class ElasticServiceIntegrationTest extends IntegrationTest {

    private static final String TEST_INDEX = createIndexName("test_index");
    private static final String TEST_ENHET = "0000";
    private static final String TEST_VEILEDER_0 = "Z000000";
    private static final String TEST_VEILEDER_1 = "Z000001";
    private static final String LITE_PRIVILEGERT_VEILEDER = "Z000001";
    private static final String UNPRIVILEGED_TOKEN = "unprivileged-test-token";
    private static final String PRIVILEGED_TOKEN = "test-token";

    private static ElasticService elasticService;
    private static ElasticIndexer elasticIndexer;


    @BeforeClass
    public static void setUp() {

        VeilarbVeilederClient veilederServiceMock = mockVeilederService();

        UnleashService unleashMock = mock(UnleashService.class);
        when(unleashMock.isEnabled(FeatureToggle.MARKER_SOM_SLETTET)).thenReturn(true);

        elasticService = new ElasticService(ELASTIC_CLIENT, veilederServiceMock, unleashMock);
        elasticIndexer = new ElasticIndexer(
                mock(AktivitetDAO.class),
                mock(BrukerRepository.class),
                ELASTIC_CLIENT,
                elasticService,
                unleashMock,
                mock(MetricsClient.class),
                mock(CvService.class)
        );
    }

    @Before
    public void createIndex() {
        elasticIndexer.opprettNyIndeks(TEST_INDEX);
    }

    @After
    public void deleteIndex() {
        elasticIndexer.slettGammelIndeks(TEST_INDEX);
    }

    @Test
    public void skal_kun_hente_ut_brukere_under_oppfolging() {

        List<OppfolgingsBruker> brukere = listOf(
                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET),

                // Markert som slettet
                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(false)
                        .setEnhet_id(TEST_ENHET)
        );

        skrivBrukereTilTestindeks(brukere);

        BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                new Filtervalg(),
                null,
                null,
                TEST_INDEX
        );


        assertThat(brukereMedAntall.getAntall()).isEqualTo(2);
    }

    @Test
    public void skal_sette_brukere_med_veileder_fra_annen_enhet_til_ufordelt() {
        List<OppfolgingsBruker> brukere = listOf(
                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(setOf("foo"))
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(setOf("foo"))
                        .setVeileder_id(TEST_VEILEDER_1)
        );

        skrivBrukereTilTestindeks(brukere);

        val filtervalg = new Filtervalg().setFerdigfilterListe(listOf(I_AVTALT_AKTIVITET));
        val response = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                filtervalg,
                null,
                null,
                TEST_INDEX
        );

        assertThat(response.getAntall()).isEqualTo(2);

        Bruker ufordeltBruker = response.getBrukere().stream()
                .filter(b -> TEST_VEILEDER_1.equals(b.getVeilederId()))
                .collect(toList()).get(0);

        assertThat(ufordeltBruker.isNyForEnhet()).isTrue();
    }

    @Test
    public void skal_hente_ut_brukere_ved_soek_paa_flere_veiledere() {
        String now = Instant.now().toString();
        List<OppfolgingsBruker> brukere = listOf(
                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setNyesteutlopteaktivitet(now)
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setNyesteutlopteaktivitet(now)
                        .setVeileder_id(TEST_VEILEDER_1),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setNyesteutlopteaktivitet(now)
                        .setVeileder_id(null)

        );

        skrivBrukereTilTestindeks(brukere);

        val filtervalg = new Filtervalg()
                .setFerdigfilterListe(listOf(UTLOPTE_AKTIVITETER))
                .setVeiledere(listOf(TEST_VEILEDER_0, TEST_VEILEDER_1));

        val response = elasticService.hentBrukere(TEST_ENHET, empty(), "asc", "ikke_satt", filtervalg, null, null, TEST_INDEX);

        assertThat(response.getAntall()).isEqualTo(2);

    }

    @Test
    public void skal_hente_riktig_antall_ufordelte_brukere() {

        List<OppfolgingsBruker> brukere = listOf(

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(null),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(null)
        );

        skrivBrukereTilTestindeks(brukere);

        val filtervalg = new Filtervalg().setFerdigfilterListe(listOf(UFORDELTE_BRUKERE));
        val response = elasticService.hentBrukere(TEST_ENHET, empty(), "asc", "ikke_satt", filtervalg, null, null, TEST_INDEX);

        assertThat(response.getAntall()).isEqualTo(2);
    }

    @Test
    public void skal_hente_riktige_antall_brukere_per_veileder() {

        val veilederId1 = "Z000000";
        val veilederId2 = "Z000001";
        val veilederId3 = "Z000003";

        List<OppfolgingsBruker> brukere = Stream.of(
                veilederId1,
                veilederId1,
                veilederId1,
                veilederId1,
                veilederId2,
                veilederId2,
                veilederId2,
                veilederId3,
                veilederId3,
                null
        )
                .map(id ->
                        new OppfolgingsBruker()
                                .setFnr(randomFnr())
                                .setVeileder_id(id)
                                .setOppfolging(true)
                                .setEnhet_id(TEST_ENHET)
                )
                .collect(toList());

        skrivBrukereTilTestindeks(brukere);
        FacetResults portefoljestorrelser = elasticService.hentPortefoljestorrelser(TEST_ENHET, TEST_INDEX);

        assertThat(facetResultCountForVeileder(veilederId1, portefoljestorrelser)).isEqualTo(4L);
        assertThat(facetResultCountForVeileder(veilederId2, portefoljestorrelser)).isEqualTo(3L);
        assertThat(facetResultCountForVeileder(veilederId3, portefoljestorrelser)).isEqualTo(2L);
    }

    @Test
    public void skal_hente_ut_riktig_antall_brukere_med_arbeidsliste() {

        val brukerMedArbeidsliste =
                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setVeileder_id(TEST_VEILEDER_0)
                        .setEnhet_id(TEST_ENHET)
                        .setArbeidsliste_aktiv(true);


        val brukerUtenArbeidsliste =
                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setVeileder_id(TEST_VEILEDER_0)
                        .setEnhet_id(TEST_ENHET)
                        .setArbeidsliste_aktiv(false);

        skrivBrukereTilTestindeks(brukerMedArbeidsliste, brukerUtenArbeidsliste);

        List<Bruker> brukereMedArbeidsliste = elasticService.hentBrukereMedArbeidsliste(TEST_VEILEDER_0, TEST_ENHET, TEST_INDEX);
        assertThat(brukereMedArbeidsliste.size()).isEqualTo(1);
    }

    @Test
    public void skal_hente_riktige_statustall_for_veileder() {

        val testBruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        val testBruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setFormidlingsgruppekode("IARBS")
                .setKvalifiseringsgruppekode("BATT")
                .setAktiviteter(setOf("egen"))
                .setArbeidsliste_aktiv(true)
                .setNy_for_enhet(true)
                .setNy_for_veileder(true)
                .setTrenger_vurdering(true)
                .setVenterpasvarfranav("2018-05-09T22:00:00Z")
                .setNyesteutlopteaktivitet("2018-05-09T22:00:00Z");

        val inaktivBruker = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setFormidlingsgruppekode("ISERV");


        skrivBrukereTilTestindeks(testBruker1, testBruker2, inaktivBruker);

        val statustall = elasticService.hentStatusTallForVeileder(TEST_VEILEDER_0, TEST_ENHET, TEST_INDEX);
        assertThat(statustall.erSykmeldtMedArbeidsgiver).isEqualTo(0);
        assertThat(statustall.iavtaltAktivitet).isEqualTo(1);
        assertThat(statustall.ikkeIavtaltAktivitet).isEqualTo(2);
        assertThat(statustall.inaktiveBrukere).isEqualTo(1);
        assertThat(statustall.minArbeidsliste).isEqualTo(1);
        assertThat(statustall.nyeBrukere).isEqualTo(1);
        assertThat(statustall.nyeBrukereForVeileder).isEqualTo(1);
        assertThat(statustall.trengerVurdering).isEqualTo(1);
        assertThat(statustall.venterPaSvarFraNAV).isEqualTo(1);
        assertThat(statustall.utlopteAktiviteter).isEqualTo(1);
    }

    @Test
    public void skal_hente_riktige_statustall_for_enhet() {

        val brukerUtenVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET);

        val brukerMedVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        skrivBrukereTilTestindeks(brukerMedVeileder, brukerUtenVeileder);

        val statustall = elasticService.hentStatusTallForEnhet(TEST_ENHET, TEST_INDEX);
        assertThat(statustall.getUfordelteBrukere()).isEqualTo(1);
    }

    @Test
    public void skal_sortere_brukere_pa_arbeidslisteikon() {

        val blaBruker = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setArbeidsliste_aktiv(true)
                .setArbeidsliste_kategori(Arbeidsliste.Kategori.BLA.name());

        val lillaBruker = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setArbeidsliste_aktiv(true)
                .setArbeidsliste_kategori(Arbeidsliste.Kategori.LILLA.name());

        skrivBrukereTilTestindeks(blaBruker, lillaBruker);

        BrukereMedAntall brukereMedAntall = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                "desc",
                "arbeidslistekategori",
                new Filtervalg(),
                null,
                null,
                TEST_INDEX
        );

        List<Bruker> brukere = brukereMedAntall.getBrukere();

        assertThat(brukere.size()).isEqualTo(2);
        assertThat(brukere.get(0).getArbeidsliste().getKategori()).isEqualTo(Arbeidsliste.Kategori.LILLA);
        assertThat(brukere.get(1).getArbeidsliste().getKategori()).isEqualTo(Arbeidsliste.Kategori.BLA);

    }

    @Test
    public void skal_hente_brukere_som_trenger_vurdering_og_er_ny_for_enhet() {

        val nyForEnhet = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_enhet(true)
                .setTrenger_vurdering(true);

        val ikkeNyForEnhet = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_enhet(true)
                .setTrenger_vurdering(false);

        skrivBrukereTilTestindeks(nyForEnhet, ikkeNyForEnhet);

        List<Brukerstatus> ferdigFiltere = listOf(
                NYE_BRUKERE,
                TRENGER_VURDERING
        );

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                "asc",
                "ikke_satt",
                new Filtervalg().setFerdigfilterListe(ferdigFiltere),
                null,
                null,
                TEST_INDEX
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(nyForEnhet, response)).isTrue();
        assertThat(userExistsInResponse(ikkeNyForEnhet, response)).isFalse();
    }

    @Test
    public void skal_ikke_kunne_hente_brukere_veileder_ikke_har_tilgang_til() {
        val brukerVeilederHarTilgangTil = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        val brukerVeilederIkkeHarTilgangTil = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id("NEGA_$testEnhet")
                .setVeileder_id("NEGA_$testVeileder");

        skrivBrukereTilTestindeks(brukerVeilederHarTilgangTil, brukerVeilederIkkeHarTilgangTil);


        val response = elasticService.hentBrukere(
                        TEST_ENHET,
                        Optional.of(TEST_VEILEDER_0),
                        "asc",
                        "ikke_satt",
                        new Filtervalg(),
                        null,
                        null,
                        TEST_INDEX
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerVeilederHarTilgangTil, response)).isTrue();
        assertThat(userExistsInResponse(brukerVeilederIkkeHarTilgangTil, response)).isFalse();
    }

    @Test
    public void skal_anse_bruker_som_ufordelt_om_bruker_har_veileder_som_ikke_har_tilgang_til_enhet() {

        val brukerMedUfordeltStatus = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
                .setNy_for_enhet(false);

        val brukerMedFordeltStatus = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_enhet(false);


        skrivBrukereTilTestindeks(brukerMedFordeltStatus, brukerMedUfordeltStatus);

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(LITE_PRIVILEGERT_VEILEDER),
                "asc",
                "ikke_satt",
                new Filtervalg().setFerdigfilterListe(listOf(UFORDELTE_BRUKERE)),
                null,
                null,
                TEST_INDEX
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(veilederExistsInResponse(LITE_PRIVILEGERT_VEILEDER, response)).isTrue();

        StatusTall statustall = elasticService.hentStatusTallForEnhet(TEST_ENHET, TEST_INDEX);
        assertThat(statustall.ufordelteBrukere).isEqualTo(1);
    }

    @Test
    public void skal_returnere_brukere_basert_på_fødselsdag_i_måneden() {
        val testBruker1 = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setFodselsdag_i_mnd(7)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        val testBruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setFodselsdag_i_mnd(8)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        skrivBrukereTilTestindeks(testBruker1, testBruker2);

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setFodselsdagIMnd(listOf("7"));

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null,
                TEST_INDEX
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(response.getBrukere().stream().anyMatch(it -> it.getFodselsdagIMnd() == 7)).isTrue();
    }

    @Test
    public void skal_hente_ut_brukere_basert_på_kjønn() {
        val mann = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setKjonn("M");

        val kvinne = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setKjonn("K");

        skrivBrukereTilTestindeks(mann, kvinne);

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setKjonn(Kjonn.K);

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null,
                TEST_INDEX
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(response.getBrukere().stream().anyMatch(bruker -> "K".equals(bruker.getKjonn()))).isTrue();
    }

    @Test
    public void skal_hente_ut_brukere_som_går_på_arbeidsavklaringspenger() {
        val brukerMedAAP = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name());

        val brukerUtenAAP = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.DAGP.name());

        skrivBrukereTilTestindeks(brukerMedAAP, brukerUtenAAP);

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setRettighetsgruppe(listOf(Rettighetsgruppe.AAP));

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null,
                TEST_INDEX
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerMedAAP, response)).isTrue();
        assertThat(userExistsInResponse(brukerUtenAAP, response)).isFalse();

    }

    @Test
    public void skal_hente_ut_brukere_filtrert_på_dagpenger_som_ytelse() {

        val brukerMedDagpengerMedPermittering = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING.name());


        val brukerMedPermitteringFiskeindustri = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI.name());

        val brukerMedAAP = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.DAGP.name())
                .setYtelse(YtelseMapping.AAP_MAXTID.name());

        val brukerMedAnnenVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI.name());

        skrivBrukereTilTestindeks(
                brukerMedDagpengerMedPermittering,
                brukerMedPermitteringFiskeindustri,
                brukerMedAAP,
                brukerMedAnnenVeileder
        );

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setYtelse(YtelseFilter.DAGPENGER);

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null,
                TEST_INDEX
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(userExistsInResponse(brukerMedDagpengerMedPermittering, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedPermitteringFiskeindustri, response)).isTrue();

    }

    @Test
    public void skal_hente_ut_brukere_som_har_avtale_om_å_søke_jobber() {
        val brukerMedSokeAvtale = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("sokeavtale"));

        val brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("behandling"));

        val brukerMedUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        skrivBrukereTilTestindeks(brukerMedSokeAvtale, brukerMedBehandling, brukerMedUtenAktiviteter);

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(mapOf(Pair.of("SOKEAVTALE", JA)));

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null,
                TEST_INDEX
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerMedSokeAvtale, response)).isTrue();
    }

    @Test
    public void skal_hente_ut_alle_brukere_unntatt_de_som_har_avtale_om_å_søke_jobber() {

        val brukerMedSokeAvtale = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("sokeavtale"));

        val brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("behandling"));

        val brukerMedUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        skrivBrukereTilTestindeks(brukerMedSokeAvtale, brukerMedBehandling, brukerMedUtenAktiviteter);

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(mapOf(Pair.of("SOKEAVTALE", AktivitetFiltervalg.NEI)));

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null,
                TEST_INDEX
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(userExistsInResponse(brukerMedBehandling, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedUtenAktiviteter, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedSokeAvtale, response)).isFalse();
    }

    @Test
    public void skal_hente_ut_alle_brukere_med_tiltak() {

        val brukerMedTiltak = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("tiltak"));

        val brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("behandling"));

        val brukerUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        skrivBrukereTilTestindeks(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter);

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(mapOf(Pair.of("TILTAK", JA)));

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null,
                TEST_INDEX
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerMedTiltak, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedBehandling, response)).isFalse();
        assertThat(userExistsInResponse(brukerUtenAktiviteter, response)).isFalse();
    }

    @Test
    public void skal_hente_ut_alle_brukere_som_ikke_har_tiltak() {
        val brukerMedTiltak = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("tiltak"))
                .setTiltak(setOf("VASV"));

        val brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("behandling"));

        val brukerUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setOppfolging(true)
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        skrivBrukereTilTestindeks(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter);

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(mapOf(Pair.of("TILTAK", AktivitetFiltervalg.NEI)));

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                filterValg,
                null,
                null,
                TEST_INDEX
        );

        assertThat(response.getAntall()).isEqualTo(2);
        assertThat(userExistsInResponse(brukerMedBehandling, response)).isTrue();
        assertThat(userExistsInResponse(brukerUtenAktiviteter, response)).isTrue();
        assertThat(userExistsInResponse(brukerMedTiltak, response)).isFalse();
    }


    private boolean veilederExistsInResponse(String veilederId, BrukereMedAntall brukere) {
        return brukere.getBrukere().stream().anyMatch(bruker -> veilederId.equals(bruker.getVeilederId()));
    }

    private boolean userExistsInResponse(OppfolgingsBruker bruker, BrukereMedAntall brukere) {
        return brukere.getBrukere().stream().anyMatch(b -> bruker.getFnr().equals(b.getFnr()));
    }

    private Long facetResultCountForVeileder(String testVeileder1, FacetResults portefoljestorrelser) {
        return portefoljestorrelser.getFacetResults().stream().filter(it -> testVeileder1.equals(it.getValue())).map(Facet::getCount).collect(toList()).get(0);
    }

    private void skrivBrukereTilTestindeks(List<OppfolgingsBruker> brukere) {
        OppfolgingsBruker[] array = new OppfolgingsBruker[brukere.size()];
        skrivBrukereTilTestindeks(brukere.toArray(array));
    }

    @SneakyThrows
    private void skrivBrukereTilTestindeks(OppfolgingsBruker... brukere) {
        elasticIndexer.skrivTilIndeks(TEST_INDEX, listOf(brukere));
        ELASTIC_CLIENT.indices().refresh(new RefreshRequest(TEST_INDEX), RequestOptions.DEFAULT);
    }

    @NotNull
    private static VeilarbVeilederClient mockVeilederService() {
        VeilarbVeilederClient veilederServiceMock = mock(VeilarbVeilederClient.class);
        when(veilederServiceMock.hentVeilederePaaEnhet(TEST_ENHET)).thenReturn(listOf((TEST_VEILEDER_0)));
        return veilederServiceMock;
    }

    @NotNull
    private static Pep mockPep() {
        Pep pepMock = mock(Pep.class);
        when(pepMock.harVeilederTilgangTilEgenAnsatt(UNPRIVILEGED_TOKEN)).thenReturn(false);
        when(pepMock.harVeilederTilgangTilKode6(UNPRIVILEGED_TOKEN)).thenReturn(false);
        when(pepMock.harVeilederTilgangTilKode7(UNPRIVILEGED_TOKEN)).thenReturn(false);
        when(pepMock.harVeilederTilgangTilEgenAnsatt(PRIVILEGED_TOKEN)).thenReturn(true);
        when(pepMock.harVeilederTilgangTilKode6(PRIVILEGED_TOKEN)).thenReturn(true);
        when(pepMock.harVeilederTilgangTilKode7(PRIVILEGED_TOKEN)).thenReturn(true);
        return pepMock;
    }

}
