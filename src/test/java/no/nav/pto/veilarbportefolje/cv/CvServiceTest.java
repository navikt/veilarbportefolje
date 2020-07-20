package no.nav.pto.veilarbportefolje.cv;

import io.vavr.control.Try;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.TestUtil;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static no.nav.common.utils.EnvironmentUtils.NAIS_NAMESPACE_PROPERTY_NAME;
import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.pto.veilarbportefolje.database.Table.BRUKER_CV.TABLE_NAME;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CvServiceTest extends IntegrationTest {
    private static JdbcTemplate jdbcTemplate;
    private static CvRepository cvRepository;

    private CvService cvService;
    private String indexName;

    private AktorregisterClient aktorregisterClient;

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(NAIS_NAMESPACE_PROPERTY_NAME, "T");
        SingleConnectionDataSource ds = TestUtil.setupInMemoryDatabase();
        jdbcTemplate = new JdbcTemplate(ds);
        cvRepository = new CvRepository(jdbcTemplate);
    }

    @Before
    public void setUp() {
        indexName = generateId();
        aktorregisterClient = mock(AktorregisterClient.class);
        cvService = new CvService(new ElasticServiceV2(ELASTIC_CLIENT), aktorregisterClient, cvRepository, mock(MetricsClient.class));
        createIndex(indexName);
    }

    @After
    public void tearDown() {
        deleteIndex(indexName);
        jdbcTemplate.execute("TRUNCATE TABLE " + TABLE_NAME);
    }

    @Test
    public void skal_hente_fnr_fra_aktoertjenesten_om_fnr_mangler_i_melding() {
        Fnr fnr = Fnr.of("00000000000");

        when(aktorregisterClient.hentFnr(anyString())).thenReturn(fnr.getFnr());

        String document = new JSONObject()
                .put("fnr", fnr.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = createDocument(indexName, fnr, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(201);

        String payload = new JSONObject()
                .put("aktoerId", "00000000000")
                .put("fnr", (Object)null)
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "CV_HJEMMEL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        GetResponse getResponse = fetchDocument(indexName, fnr);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isTrue();
    }

    @Test
    public void skal_oppdatere_dokumentet_i_db_og_elastic() {
        Fnr fnr = Fnr.of("00000000000");

        String document = new JSONObject()
                .put("fnr", fnr.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = createDocument(indexName, fnr, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(201);

        String aktoerId = "1";
        String payload = new JSONObject()
                .put("aktoerId", aktoerId)
                .put("fnr", fnr)
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "CV_HJEMMEL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        String harDeltCvDb = cvRepository.harDeltCv(AktoerId.of(aktoerId));
        assertThat(harDeltCvDb).isEqualTo("J");

        GetResponse getResponse = fetchDocument(indexName, fnr);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isTrue();
    }

    @Test
    public void skal_ikke_behandle_meldinger_som_har_meldingstype_arbeidsgiver_generell() {
        Fnr fnr = Fnr.of("00000000000");

        String document = new JSONObject()
                .put("fnr", fnr.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = createDocument(indexName, fnr, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(201);

        String payload = new JSONObject()
                .put("aktoerId", "00000000000")
                .put("fnr", fnr)
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "ARBEIDSGIVER_GENERELL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        GetResponse getResponse = fetchDocument(indexName, fnr);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isFalse();
    }

    @Test
    public void skal_ikke_behandle_meldinger_som_har_meldingstype_cv_generell() {
        Fnr fnr = Fnr.of("00000000000");
        String document = new JSONObject()
                .put("fnr", fnr.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = createDocument(indexName, fnr, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(201);

        String payload = new JSONObject()
                .put("aktoerId", "00000000000")
                .put("fnr", fnr)
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "CV_GENERELL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        GetResponse getResponse = fetchDocument(indexName, fnr);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isFalse();
    }

    @Test
    public void skal_ignorere_tilfeller_hvor_dokumentet_ikke_finnes_i_elastic() {
        Fnr fnr = Fnr.of("00000000000");
        String payload = new JSONObject()
                .put("aktoerId", "00000000000")
                .put("fnr", fnr)
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "CV_HJEMMEL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        GetResponse getResponse = fetchDocument(indexName, fnr);
        assertThat(getResponse.isExists()).isFalse();
    }

}
