package no.nav.pto.veilarbportefolje.registrering;

import lombok.SneakyThrows;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.arbeid.soker.registrering.UtdanningBestattSvar;
import no.nav.arbeid.soker.registrering.UtdanningGodkjentSvar;
import no.nav.arbeid.soker.registrering.UtdanningSvar;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.TestUtil;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Filtervalg;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.elastic.ElasticService;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.kafka.IntegrationTest;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.RequestOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static java.util.Optional.empty;
import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.pto.veilarbportefolje.util.CollectionUtils.listOf;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

public class RegistreringIntegrationTest extends IntegrationTest {
    private static JdbcTemplate jdbcTemplate;
    private static RegistreringRepository registreringRepository;
    private static NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static ElasticService elasticService;
    private static ElasticIndexer elasticIndexer;
    private static final String TEST_ENHET = "0000";
    private static final String TEST_VEILEDER_0 = "Z000000";

    private static RegistreringService registreringService;
    private static String indexName;
    private static AktoerId AKTORID = AktoerId.of("0000000000");
    private static Fnr fnr = Fnr.of("11111111111");

    @BeforeClass
    public static void beforeClass() {
        SingleConnectionDataSource ds = TestUtil.setupInMemoryDatabase();
        jdbcTemplate = new JdbcTemplate(ds);
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(ds);
        registreringRepository = new RegistreringRepository(jdbcTemplate);
    }

    @Before
    public void setUp() {
        indexName = generateId();

        BrukerRepository brukerRepository = new BrukerRepository(jdbcTemplate,namedParameterJdbcTemplate);
        BrukerRepository brukerRepositorySpy = spy(brukerRepository);
        Mockito.doReturn(Optional.of(fnr.toString())).when(brukerRepositorySpy).hentFnrView(AKTORID);

        registreringService = new RegistreringService(registreringRepository, new ElasticServiceV2(ELASTIC_CLIENT, indexName), brukerRepositorySpy);
        elasticService = new ElasticService(ELASTIC_CLIENT, mockVeilederService(), mock(UnleashService.class), indexName);
        elasticIndexer = new ElasticIndexer(
                new AktivitetDAO(jdbcTemplate,namedParameterJdbcTemplate),
                brukerRepository,
                ELASTIC_CLIENT,
                mock(UnleashService.class),
                mock(MetricsClient.class),
                indexName
        );
        elasticIndexer.opprettNyIndeks(indexName);
    }

    @After
    public void tearDown() {
        deleteIndex(indexName);
        jdbcTemplate.execute("TRUNCATE TABLE BRUKER_REGISTRERING");
    }


    @Test
    public void utdanning_full_integration() {
        populateSingleBruker();

        ArbeidssokerRegistrertEvent kafkaMessage = ArbeidssokerRegistrertEvent.newBuilder()
                .setAktorid(AKTORID.toString())
                .setBrukersSituasjon("Permittert")
                .setUtdanning(UtdanningSvar.GRUNNSKOLE)
                .setUtdanningBestatt(UtdanningBestattSvar.INGEN_SVAR)
                .setUtdanningGodkjent(UtdanningGodkjentSvar.JA)
                .setRegistreringOpprettet(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()).format(ISO_ZONED_DATE_TIME))
                .build();

        registreringService.behandleKafkaMelding(kafkaMessage);

        GetResponse getResponse = fetchDocument(indexName, fnr);
        assertThat(getResponse.isExists()).isTrue();

        String utdanning = (String) getResponse.getSourceAsMap().get("utdanning");
        String situasjon = (String) getResponse.getSourceAsMap().get("brukers_situasjon");
        String utdanningBestatt = (String) getResponse.getSourceAsMap().get("utdanning_bestatt");
        String utdanningGodkjent = (String) getResponse.getSourceAsMap().get("utdanning_godkjent");

        assertThat(utdanning).isEqualTo(UtdanningSvar.GRUNNSKOLE.toString());
        assertThat(situasjon).isEqualTo("Permittert");
        assertThat(utdanningBestatt).isEqualTo(UtdanningBestattSvar.INGEN_SVAR.toString());
        assertThat(utdanningGodkjent).isEqualTo(UtdanningGodkjentSvar.JA.toString());
    }

    @Test
    public void utdanning_filter_test() {
        populateElastic();
        var responseBrukere = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                new Filtervalg(),
                null,
                null);

        assertThat(responseBrukere.getAntall()).isEqualTo(3);
        var responseBrukere2 = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgBestatt(),
                null,
                null);

        assertThat(responseBrukere2.getAntall()).isEqualTo(1);

        var responseBrukere3 = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgGodkjent(),
                null,
                null);

        assertThat(responseBrukere3.getAntall()).isEqualTo(2);

        var responseBrukere4 = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgUtdanning(),
                null,
                null);

        assertThat(responseBrukere4.getAntall()).isEqualTo(2);

        var responseBrukere5 = elasticService.hentBrukere(
                TEST_ENHET,
                empty(),
                "asc",
                "ikke_satt",
                getFiltervalgMix(),
                null,
                null);

        assertThat(responseBrukere5.getAntall()).isEqualTo(1);
    }

    private static VeilarbVeilederClient mockVeilederService() {
        VeilarbVeilederClient veilederServiceMock = mock(VeilarbVeilederClient.class);
        when(veilederServiceMock.hentVeilederePaaEnhet(TEST_ENHET)).thenReturn(listOf((TEST_VEILEDER_0)));
        return veilederServiceMock;
    }

    private static Filtervalg getFiltervalgBestatt(){
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>()); // TODO: Denne må være der, er det en bug?
        filtervalg.utdanningBestatt.add("JA");
        return filtervalg;
    }

    private static Filtervalg getFiltervalgGodkjent(){
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanningGodkjent.add("JA");
        return filtervalg;
    }

    private static Filtervalg getFiltervalgUtdanning(){
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanning.add("GRUNNSKOLE");
        return filtervalg;
    }

    private static Filtervalg getFiltervalgMix(){
        Filtervalg filtervalg = new Filtervalg();
        filtervalg.setFerdigfilterListe(new ArrayList<>());
        filtervalg.utdanning.add("GRUNNSKOLE");
        filtervalg.utdanningGodkjent.add("JA");
        filtervalg.utdanningBestatt.add("NEI");
        return filtervalg;
    }

    private static void populateSingleBruker() {
        OppfolgingsBruker bruker =
                new OppfolgingsBruker()
                        .setFnr(fnr.toString())
                        .setAktoer_id(AKTORID.toString())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET);

        skrivBrukereTilTestindeks(bruker);
        pollUntilHarOppdatertIElastic(()-> countDocuments(indexName) < 1);
    }

    private static void populateElastic() {
        List<OppfolgingsBruker> brukere = listOf(
                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setUtdanning_bestatt("NEI")
                        .setUtdanning_godkjent("NEI"),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setUtdanning_bestatt("JA")
                        .setUtdanning_godkjent("JA")
                        .setUtdanning("GRUNNSKOLE"),

                new OppfolgingsBruker()
                        .setFnr(randomFnr())
                        .setOppfolging(true)
                        .setEnhet_id(TEST_ENHET)
                        .setUtdanning_bestatt("NEI")
                        .setUtdanning_godkjent("JA")
                        .setUtdanning("GRUNNSKOLE")
        );

        skrivBrukereTilTestindeks(brukere);
        pollUntilHarOppdatertIElastic(()-> countDocuments(indexName) < brukere.size());
    }

    private static void skrivBrukereTilTestindeks(List<OppfolgingsBruker> brukere) {
        OppfolgingsBruker[] array = new OppfolgingsBruker[brukere.size()];
        skrivBrukereTilTestindeks(brukere.toArray(array));
    }

    @SneakyThrows
    private static void skrivBrukereTilTestindeks(OppfolgingsBruker... brukere) {
        elasticIndexer.skrivTilIndeks(indexName, listOf(brukere));
        ELASTIC_CLIENT.indices().refreshAsync(new RefreshRequest(indexName), RequestOptions.DEFAULT, new ActionListener<RefreshResponse>() {
            @Override
            public void onResponse(RefreshResponse refreshResponse) {
                System.out.println("refreshed");
            }

            @Override
            public void onFailure(Exception e) {
                System.err.println("noe gikk galt her " + e);
            }
        });
    }
}
