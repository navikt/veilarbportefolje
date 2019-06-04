package no.nav.fo.veilarbportefolje.indeksering;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.brukerdialog.security.context.SubjectRule;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.common.utils.Pair;
import no.nav.fasit.FasitUtils;
import no.nav.fasit.ServiceUser;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.domene.*;
import no.nav.fo.veilarbportefolje.indeksering.domene.ElasticClientConfig;
import no.nav.fo.veilarbportefolje.indeksering.domene.OppfolgingsBruker;
import no.nav.fo.veilarbportefolje.service.PepClient;
import no.nav.fo.veilarbportefolje.service.VeilederService;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static no.nav.brukerdialog.security.domain.IdentType.InternBruker;
import static no.nav.common.auth.SsoToken.oidcToken;
import static no.nav.common.utils.CollectionUtils.*;
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.*;
import static no.nav.fo.veilarbportefolje.domene.AktivitetFiltervalg.JA;
import static no.nav.fo.veilarbportefolje.domene.AktivitetFiltervalg.NEI;
import static no.nav.fo.veilarbportefolje.domene.Brukerstatus.*;
import static no.nav.fo.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Ignore
@Slf4j
public class ElasticServiceIntegrationTest {

    private static ElasticService elasticService;
    private static ElasticIndexer indexer;

    private static String TEST_INDEX = IndekseringUtils.createIndexName("testindeks");
    private static String TEST_ENHET = "0000";
    private static String TEST_VEILEDER_0 = "Z000000";
    private static String TEST_VEILEDER_1 = "Z000001";
    private static String LITE_PRIVILEGERT_VEILEDER = "Z000001";
    private static String UNPRIVILEGED_TOKEN = "unprivileged-test-token";
    private static String PRIVILEGED_TOKEN = "test-token";

    @BeforeClass
    public static void beforeClass() {

        ServiceUser user = FasitUtils.getServiceUser("veilarbelastic_user", APPLICATION_NAME);
        System.setProperty(ELASTICSEARCH_USERNAME_PROPERTY, user.username);
        System.setProperty(ELASTICSEARCH_PASSWORD_PROPERTY, user.password);
        System.setProperty("FASIT_ENVIRONMENT_NAME", FasitUtils.getDefaultEnvironment());

        PepClient pepMock = mock(PepClient.class);
        when(pepMock.isSubjectAuthorizedToSeeEgenAnsatt(UNPRIVILEGED_TOKEN)).thenReturn(false);
        when(pepMock.isSubjectAuthorizedToSeeKode6(UNPRIVILEGED_TOKEN)).thenReturn(false);
        when(pepMock.isSubjectAuthorizedToSeeKode7(UNPRIVILEGED_TOKEN)).thenReturn(false);
        when(pepMock.isSubjectAuthorizedToSeeEgenAnsatt(PRIVILEGED_TOKEN)).thenReturn(true);
        when(pepMock.isSubjectAuthorizedToSeeKode6(PRIVILEGED_TOKEN)).thenReturn(true);
        when(pepMock.isSubjectAuthorizedToSeeKode7(PRIVILEGED_TOKEN)).thenReturn(true);

        VeilederService veilederServiceMock = mock(VeilederService.class);
        when(veilederServiceMock.getIdenter(TEST_ENHET)).thenReturn(listOf(VeilederId.of(TEST_VEILEDER_0)));

        RestHighLevelClient restClient = ElasticConfig.createClient(ElasticClientConfig.builder()
                .username("")
                .password("")
                .hostname("localhost")
                .port(9200)
                .scheme("http")
                .build()
        );

        elasticService = new ElasticService(restClient, pepMock, veilederServiceMock);

        indexer = new ElasticIndexer(
                mock(AktivitetDAO.class),
                mock(BrukerRepository.class),
                restClient,
                elasticService
        );

    }

    @Before
    public void setUp() {
        indexer.opprettNyIndeks(TEST_INDEX);
    }

    @After
    public void tearDown() {
        indexer.slettGammelIndeks(TEST_INDEX);
    }

    @Rule
    public SubjectRule subjectRule = new SubjectRule(new Subject(TEST_VEILEDER_0, InternBruker, oidcToken(PRIVILEGED_TOKEN, emptyMap())));


    @Test
    public void skal_filtrere_paa_valgte_aktiviteter() {
        String fnr1 = randomFnr();
        String fnr2 = randomFnr();
        String fnr3 = randomFnr();
        String fnr4 = randomFnr();
        String fnr5 = randomFnr();
        String fnr6 = randomFnr();

        List<OppfolgingsBruker> brukere = listOf(
                new OppfolgingsBruker()
                        .setFnr(fnr1)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(setOf("egen"))
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(fnr2)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(setOf("behandling", "stilling"))
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(fnr3)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(setOf("stilling"))
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(fnr4)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(setOf("tiltak"))
                        .setTiltak(setOf("foo", "bar"))
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(fnr5)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(setOf("tiltak", "egen"))
                        .setTiltak(setOf("foo", "bar"))
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(fnr6)
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(TEST_VEILEDER_0)
        );

        skrivBrukereTilTestindeks(brukere);

        val filtervalg = new Filtervalg()
                .setFerdigfilterListe(listOf(I_AVTALT_AKTIVITET))
                .setAktiviteter(mapOf(
                        Pair.of("STILLING", JA),
                        Pair.of("EGEN", JA),
                        Pair.of("BEHANDLING", NEI)
                ));

        val response = elasticService.hentBrukere(TEST_ENHET, Optional.of(TEST_VEILEDER_0), "ascending", "valgteaktiviteter", filtervalg, null, null, TEST_INDEX);

        assertThat(response.getAntall()).isEqualTo(3);

        val filtervalg2 = new Filtervalg()
                .setFerdigfilterListe(listOf(I_AVTALT_AKTIVITET))
                .setAktiviteter(mapOf(
                        Pair.of("TILTAK", NEI)
                ));

        val response2 = elasticService.hentBrukere(TEST_ENHET, Optional.of(TEST_VEILEDER_0), "ascending", "valgteaktiviteter", filtervalg2, null, null, TEST_INDEX);
        assertThat(response2.getAntall()).isEqualTo(4);
    }

    @Test
    public void skal_ikke_sortere_paa_aktivitetstype() {

        Instant now = Instant.now();

        String first = now.minusSeconds(240).toString();
        String second = now.minusSeconds(120).toString();
        String last = now.toString();

        String firstUserFnr = randomFnr();
        String secondFnr = randomFnr();
        String lastUserFnr = randomFnr();

        List<OppfolgingsBruker> brukere = listOf(
                new OppfolgingsBruker()
                        .setFnr(secondFnr)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(setOf("egen"))
                        .setAktivitet_egen_utlopsdato(second)
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(firstUserFnr)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(setOf("behandling, stilling"))
                        .setAktivitet_behandling_utlopsdato(first)
                        .setAktivitet_stilling_utlopsdato(last)
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(lastUserFnr)
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(setOf("stilling"))
                        .setAktivitet_stilling_utlopsdato(last)
                        .setVeileder_id(TEST_VEILEDER_0)

        );

        skrivBrukereTilTestindeks(brukere);

        val filtervalg = new Filtervalg()
                .setFerdigfilterListe(listOf(I_AVTALT_AKTIVITET));

        val ascendingSortResponse = elasticService.hentBrukere(TEST_ENHET, Optional.of(TEST_VEILEDER_0), "ascending", "iavtaltaktivitet", filtervalg, null, null, TEST_INDEX);

        assertThat(ascendingSortResponse.getAntall()).isEqualTo(3);

        assertThat(ascendingSortResponse.getBrukere().get(0).getFnr()).isEqualTo(firstUserFnr);
        assertThat(ascendingSortResponse.getBrukere().get(1).getFnr()).isEqualTo(secondFnr);
        assertThat(ascendingSortResponse.getBrukere().get(2).getFnr()).isEqualTo(lastUserFnr);

        val descendingSortResponse = elasticService.hentBrukere(TEST_ENHET, Optional.of(TEST_VEILEDER_0), "descending", "iavtaltaktivitet", filtervalg, null, null, TEST_INDEX);

        assertThat(descendingSortResponse.getAntall()).isEqualTo(3);
        assertThat(descendingSortResponse.getBrukere().get(2).getFnr()).isEqualTo(firstUserFnr);
        assertThat(descendingSortResponse.getBrukere().get(1).getFnr()).isEqualTo(secondFnr);
        assertThat(descendingSortResponse.getBrukere().get(0).getFnr()).isEqualTo(lastUserFnr);

    }

    @Test
    public void skal_sette_brukere_med_veileder_fra_annen_enhet_til_ufordelt() {
        List<OppfolgingsBruker> brukere = listOf(
                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(setOf("foo"))
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setEnhet_id(TEST_ENHET)
                        .setAktiviteter(setOf("foo"))
                        .setVeileder_id(TEST_VEILEDER_1)
        );

        skrivBrukereTilTestindeks(brukere);

        val filtervalg = new Filtervalg()
                .setFerdigfilterListe(listOf(I_AVTALT_AKTIVITET));

        val response = elasticService.hentBrukere(TEST_ENHET, Optional.empty(), "ascending", "ikke_satt", filtervalg, null, null, TEST_INDEX);

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
                        .setEnhet_id(TEST_ENHET)
                        .setNyesteutlopteaktivitet(now)
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setEnhet_id(TEST_ENHET)
                        .setNyesteutlopteaktivitet(now)
                        .setVeileder_id(TEST_VEILEDER_1),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setEnhet_id(TEST_ENHET)
                        .setNyesteutlopteaktivitet(now)
                        .setVeileder_id(null)

        );

        skrivBrukereTilTestindeks(brukere);

        val filtervalg = new Filtervalg()
                .setFerdigfilterListe(listOf(UTLOPTE_AKTIVITETER))
                .setVeiledere(listOf(TEST_VEILEDER_0, TEST_VEILEDER_1));

        val response = elasticService.hentBrukere(TEST_ENHET, Optional.empty(), "ascending", "ikke_satt", filtervalg, null, null, TEST_INDEX);

        assertThat(response.getAntall()).isEqualTo(2);

    }

    @Test
    public void skal_hente_riktig_antall_ufordelte_brukere() {

        List<OppfolgingsBruker> brukere = listOf(

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(null),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(TEST_VEILEDER_0),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setEnhet_id(TEST_ENHET)
                        .setVeileder_id(null)
        );

        skrivBrukereTilTestindeks(brukere);

        val filtervalg = new Filtervalg().setFerdigfilterListe(listOf(UFORDELTE_BRUKERE));
        val response = elasticService.hentBrukere(TEST_ENHET, Optional.empty(), "ascending", "ikke_satt", filtervalg, null, null, TEST_INDEX);

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
                        .setVeileder_id(TEST_VEILEDER_0)
                        .setEnhet_id(TEST_ENHET)
                        .setArbeidsliste_aktiv(true);


        val brukerUtenArbeidsliste =
                new OppfolgingsBruker()
                        .setFnr(randomFnr())
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
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        val testBruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr())
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
                .setEnhet_id(TEST_ENHET);

        val brukerMedVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        skrivBrukereTilTestindeks(brukerMedVeileder, brukerUtenVeileder);

        val statustall = elasticService.hentStatusTallForEnhet(TEST_ENHET, TEST_INDEX);
        assertThat(statustall.ufordelteBrukere).isEqualTo(1);
    }

    @Test
    public void skal_hente_brukere_som_trenger_vurdering_og_er_ny_for_enhet() {

        val nyForEnhet = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_enhet(true)
                .setTrenger_vurdering(true);

        val ikkeNyForEnhet = new OppfolgingsBruker()
                .setFnr(randomFnr())
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
                "ascending",
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
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        val brukerVeilederIkkeHarTilgangTil = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setEnhet_id("NEGA_$testEnhet")
                .setVeileder_id("NEGA_$testVeileder");

        skrivBrukereTilTestindeks(brukerVeilederHarTilgangTil, brukerVeilederIkkeHarTilgangTil);

        val unprivelegedSubject = new Subject(UNPRIVILEGED_TOKEN, InternBruker, oidcToken(UNPRIVILEGED_TOKEN, emptyMap()));

        val response = SubjectHandler.withSubject(
                unprivelegedSubject,
                () -> elasticService.hentBrukere(
                        TEST_ENHET,
                        Optional.of(TEST_VEILEDER_0),
                        "ascending",
                        "ikke_satt",
                        new Filtervalg(),
                        null,
                        null,
                        TEST_INDEX
                )
        );

        assertThat(response.getAntall()).isEqualTo(1);
        assertThat(userExistsInResponse(brukerVeilederHarTilgangTil, response)).isTrue();
        assertThat(userExistsInResponse(brukerVeilederIkkeHarTilgangTil, response)).isFalse();
    }

    @Test
    public void skal_anse_bruker_som_ufordelt_om_bruker_har_veileder_som_ikke_har_tilgang_til_enhet() {

        val brukerMedUfordeltStatus = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(LITE_PRIVILEGERT_VEILEDER)
                .setNy_for_enhet(false);

        val brukerMedFordeltStatus = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setNy_for_enhet(false);


        skrivBrukereTilTestindeks(brukerMedFordeltStatus, brukerMedUfordeltStatus);

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(LITE_PRIVILEGERT_VEILEDER),
                "ascending",
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
                .setFodselsdag_i_mnd(7)
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0);

        val testBruker2 = new OppfolgingsBruker()
                .setFnr(randomFnr())
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
                "ascending",
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
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setKjonn("M");

        val kvinne = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setKjonn("K");

        skrivBrukereTilTestindeks(mann, kvinne);

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setKjonn(listOf(Kjonn.K));

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.of(TEST_VEILEDER_0),
                "ascending",
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
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name());

        val brukerUtenAAP = new OppfolgingsBruker()
                .setFnr(randomFnr())
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
                "ascending",
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
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING.name());


        val brukerMedPermitteringFiskeindustri = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.AAP.name())
                .setYtelse(YtelseMapping.DAGPENGER_MED_PERMITTERING_FISKEINDUSTRI.name());

        val brukerMedAAP = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setEnhet_id(TEST_ENHET)
                .setVeileder_id(TEST_VEILEDER_0)
                .setRettighetsgruppekode(Rettighetsgruppe.DAGP.name())
                .setYtelse(YtelseMapping.AAP_MAXTID.name());

        val brukerMedAnnenVeileder = new OppfolgingsBruker()
                .setFnr(randomFnr())
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
                "ascending",
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
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("sokeavtale"));

        val brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("behandling"));

        val brukerMedUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        skrivBrukereTilTestindeks(brukerMedSokeAvtale, brukerMedBehandling, brukerMedUtenAktiviteter);

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(mapOf(Pair.of("SOKEAVTALE", JA)));

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                "ascending",
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
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("sokeavtale"));

        val brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("behandling"));

        val brukerMedUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        skrivBrukereTilTestindeks(brukerMedSokeAvtale, brukerMedBehandling, brukerMedUtenAktiviteter);

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(mapOf(Pair.of("SOKEAVTALE", NEI)));

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                "ascending",
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
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("tiltak"));

        val brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("behandling"));

        val brukerUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        skrivBrukereTilTestindeks(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter);

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(mapOf(Pair.of("TILTAK", AktivitetFiltervalg.JA)));

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                "ascending",
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
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("tiltak"))
                .setTiltak(setOf("VASV"));

        val brukerMedBehandling = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET)
                .setAktiviteter(setOf("behandling"));

        val brukerUtenAktiviteter = new OppfolgingsBruker()
                .setFnr(randomFnr())
                .setVeileder_id(TEST_VEILEDER_0)
                .setEnhet_id(TEST_ENHET);

        skrivBrukereTilTestindeks(brukerMedTiltak, brukerMedBehandling, brukerUtenAktiviteter);

        val filterValg = new Filtervalg()
                .setFerdigfilterListe(emptyList())
                .setAktiviteter(mapOf(Pair.of("TILTAK", NEI)));

        val response = elasticService.hentBrukere(
                TEST_ENHET,
                Optional.empty(),
                "ascending",
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

    private static void skrivBrukereTilTestindeks(List<OppfolgingsBruker> brukere) {
        OppfolgingsBruker[] array = new OppfolgingsBruker[brukere.size()];
        skrivBrukereTilTestindeks(brukere.toArray(array));
    }

    @SneakyThrows
    private static void skrivBrukereTilTestindeks(OppfolgingsBruker... brukere) {
        indexer.skrivTilIndeks(TEST_INDEX, listOf(brukere));
        Thread.sleep(1000); // Gi elastic litt tid på å indeksere dataene
    }

}
