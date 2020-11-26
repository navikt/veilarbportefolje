package no.nav.pto.veilarbportefolje.arbeidsliste;

import io.vavr.control.Try;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.TestUtil;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.auth.AuthService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.Table;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.kafka.IntegrationTest;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.junit.*;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static io.vavr.control.Validation.valid;
import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.pto.veilarbportefolje.util.CollectionUtils.listOf;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.mockito.Mockito.mock;

public class ArbeidslisteIntegrationTest extends IntegrationTest {

    private static NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static JdbcTemplate jdbcTemplate;
    private static AktorregisterClient aktorregisterClientMock;
    private static AuthService authMock;

    private ArbeidslisteService arbeidslisteService;
    private ArbeidslisteRepository arbeidslisteRepository;

    private String indexName;

    private static final String TEST_ENHET = "0000";
    private static final String TEST_VEILEDER_0 = "Z000000";
    private static AktoerId aktoerId = AktoerId.of("0000000000");
    private static Fnr fnr = Fnr.of("11111111111");
    private ElasticIndexer elasticIndexer;
    private BrukerService brukerService;
    @BeforeClass
    public static void beforeClass() {
        SingleConnectionDataSource ds = TestUtil.setupInMemoryDatabase();
        jdbcTemplate = new JdbcTemplate(ds);
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds);
        aktorregisterClientMock = mock(AktorregisterClient.class);
        authMock = mock(AuthService.class);
        Mockito.doReturn(true).when(authMock).harVeilederTilgangTilEnhet(Mockito.anyString(), Mockito.anyString());

    }

    @Before
    public void setUp() {
        indexName = generateId();
        arbeidslisteRepository = new ArbeidslisteRepository(jdbcTemplate, namedParameterJdbcTemplate);
        BrukerRepository brukerRepository = new BrukerRepository(jdbcTemplate, namedParameterJdbcTemplate);
        brukerService = Mockito.spy( new BrukerService(brukerRepository, aktorregisterClientMock));
        arbeidslisteService = Mockito.spy(new ArbeidslisteService(aktorregisterClientMock, arbeidslisteRepository, brukerService,new ElasticServiceV2(ELASTIC_CLIENT, indexName), mock(MetricsClient.class)));

        elasticIndexer = new ElasticIndexer(
                new AktivitetDAO(jdbcTemplate,namedParameterJdbcTemplate),
                brukerRepository,
                ELASTIC_CLIENT,
                mock(UnleashService.class),
                mock(MetricsClient.class),
                indexName
        );
        elasticIndexer.opprettNyIndeks(indexName);
        jdbcTemplate.execute("TRUNCATE TABLE " + Table.ARBEIDSLISTE.TABLE_NAME);
        jdbcTemplate.execute("TRUNCATE TABLE " + Table.OPPFOLGINGSBRUKER.TABLE_NAME);
        jdbcTemplate.execute("TRUNCATE TABLE " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);
    }

    @After
    public void tearDown() {
        deleteIndex(indexName);
        jdbcTemplate.execute("TRUNCATE TABLE " + Table.ARBEIDSLISTE.TABLE_NAME);
        jdbcTemplate.execute("TRUNCATE TABLE " + Table.OPPFOLGINGSBRUKER.TABLE_NAME);
        jdbcTemplate.execute("TRUNCATE TABLE " + Table.AKTOERID_TO_PERSONID.TABLE_NAME);
    }

    @Test
    public void lage_og_hent_arbeidsliste() {
        Mockito.doReturn(valid(fnr)).when(arbeidslisteService).erVeilederForBruker(Mockito.anyString());
        Mockito.doReturn(Optional.of(TEST_ENHET)).when(brukerService).hentNavKontorFraDbLinkTilArena(Mockito.any(Fnr.class));

        populateElastic();

        ArbeidslisteDTO localArbeidsliste = new ArbeidslisteDTO(fnr)
                .setAktoerId(aktoerId)
                .setVeilederId(VeilederId.of(TEST_VEILEDER_0))
                .setFrist(null)
                .setKommentar("Dette er en kommentar")
                .setOverskrift("Dette er en overskrift")
                .setKategori(Arbeidsliste.Kategori.BLA);

        Try<ArbeidslisteDTO> arbeidsliste = arbeidslisteService.createArbeidsliste(localArbeidsliste);
        Assert.assertEquals(arbeidsliste.get(), localArbeidsliste);

         Optional<Arbeidsliste> ab = arbeidslisteService.getArbeidsliste(fnr);
         Assert.assertEquals(mockArbeidslisteFromDTO_IgnoreEndringstidspunkt(ab.get(),localArbeidsliste),ab.get());

    }

    @Test
    public void skal_oppdatere_arbeidsliste() {
        Mockito.doReturn(valid(fnr)).when(arbeidslisteService).erVeilederForBruker(Mockito.anyString());
        Mockito.doReturn(Optional.of(TEST_ENHET)).when(brukerService).hentNavKontorFraDbLinkTilArena(Mockito.any(Fnr.class));

        populateElastic();

        ArbeidslisteDTO localArbeidsliste_1 = new ArbeidslisteDTO(fnr)
                .setAktoerId(aktoerId)
                .setVeilederId(VeilederId.of(TEST_VEILEDER_0))
                .setFrist(null)
                .setKommentar("Dette er en kommentar")
                .setOverskrift("Dette er en overskrift")
                .setKategori(Arbeidsliste.Kategori.BLA);

        ArbeidslisteDTO localArbeidsliste_2 = new ArbeidslisteDTO(fnr)
                .setAktoerId(aktoerId)
                .setVeilederId(VeilederId.of(TEST_VEILEDER_0))
                .setFrist(null)
                .setKommentar("Dette er en oppdatert kommentar")
                .setOverskrift("Dette er en oppdatert overskrift")
                .setKategori(Arbeidsliste.Kategori.GRONN);

        Try<ArbeidslisteDTO> arbeidsliste_1 = arbeidslisteService.createArbeidsliste(localArbeidsliste_1);
        Try<ArbeidslisteDTO> arbeidsliste_2 = arbeidslisteService.updateArbeidsliste(localArbeidsliste_2);


        Optional<Arbeidsliste> ab = arbeidslisteService.getArbeidsliste(fnr);

        Assert.assertEquals(localArbeidsliste_1, arbeidsliste_1.get());
        Assert.assertNotNull(arbeidsliste_2.get().endringstidspunkt);

        Assert.assertEquals(mockArbeidslisteFromDTO_IgnoreEndringstidspunkt(ab.get(),localArbeidsliste_2),ab.get());
    }

    @Test
    public void skal_slette_arbeidsliste() {
        Mockito.doReturn(valid(fnr)).when(arbeidslisteService).erVeilederForBruker(Mockito.anyString());
        Mockito.doReturn(Optional.of(TEST_ENHET)).when(brukerService).hentNavKontorFraDbLinkTilArena(Mockito.any(Fnr.class));

        populateElastic();

        ArbeidslisteDTO localArbeidsliste = new ArbeidslisteDTO(fnr)
                .setAktoerId(aktoerId)
                .setVeilederId(VeilederId.of(TEST_VEILEDER_0))
                .setFrist(null)
                .setKommentar("Dette er en kommentar")
                .setOverskrift("Dette er en overskrift")
                .setKategori(Arbeidsliste.Kategori.BLA);

        Try<ArbeidslisteDTO> arbeidsliste_1 = arbeidslisteService.createArbeidsliste(localArbeidsliste);
        Try<AktoerId> deleted_id = arbeidslisteService.deleteArbeidsliste(fnr);


        Optional<Arbeidsliste> ab = arbeidslisteService.getArbeidsliste(fnr);

        Assert.assertEquals(localArbeidsliste, arbeidsliste_1.get());
        Assert.assertEquals(localArbeidsliste.aktoerId, deleted_id.get());
        Assert.assertTrue(ab.isEmpty());
    }

    private Arbeidsliste mockArbeidslisteFromDTO_IgnoreEndringstidspunkt(Arbeidsliste arbeidsliste, ArbeidslisteDTO arbeidslisteDTO){
        ZonedDateTime frist = arbeidslisteDTO.frist == null ? null : ZonedDateTime.of(arbeidslisteDTO.frist.toLocalDateTime(), ZoneId.of("UTC"));
        return new Arbeidsliste(arbeidslisteDTO.veilederId,
                arbeidsliste.endringstidspunkt,
                arbeidslisteDTO.overskrift,
                arbeidslisteDTO.kommentar,
                frist,
                arbeidslisteDTO.kategori);
    }

    private void populateElastic() {
        List<OppfolgingsBruker> brukere = listOf(
                new OppfolgingsBruker()
                        .setFnr(fnr.toString())
                        .setAktoer_id(aktoerId.toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
        );

        skrivBrukereTilTestindeks(indexName, elasticIndexer, brukere);
        pollUntilHarOppdatertIElastic(()-> countDocuments(indexName) < brukere.size());
    }

}
