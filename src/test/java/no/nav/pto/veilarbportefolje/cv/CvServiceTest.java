package no.nav.pto.veilarbportefolje.cv;

import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static no.nav.common.utils.IdUtils.generateId;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class CvServiceTest extends IntegrationTest {
    private CvService cvService;
    private String indexName;

    @Before
    public void setUp() {
        indexName = generateId();
        cvService = new CvService(new ElasticServiceV2(ELASTIC_CLIENT, indexName));
        createIndex(indexName);
    }

    @After
    public void tearDown() {
        deleteIndex(indexName);
    }

    @Test
    public void skal_oppdatere_dokumentet_i_elastic() {
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
                .put("ressurs", "CV_HJEMMEL")
                .toString();

        cvService.behandleKafkaMelding(payload);

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
