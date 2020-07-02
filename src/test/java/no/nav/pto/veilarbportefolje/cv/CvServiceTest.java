package no.nav.pto.veilarbportefolje.cv;

import io.vavr.control.Try;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static no.nav.common.utils.IdUtils.generateId;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CvServiceTest extends IntegrationTest {
    private CvService cvService;
    private String indexName;

    private AktoerService aktoerServiceMock;

    @Before
    public void setUp() {
        indexName = generateId();
        aktoerServiceMock = mock(AktoerService.class);
        cvService = new CvService(new ElasticServiceV2(ELASTIC_CLIENT, indexName), aktoerServiceMock);
        createIndex(indexName);
    }

    @After
    public void tearDown() {
        deleteIndex(indexName);
    }

    @Test
    public void skal_hente_fnr_fra_aktoertjenesten_om_fnr_mangler_i_melding() {
        Fnr fnr = Fnr.of("00000000000");

        when(aktoerServiceMock.hentFnrFraAktorId(any(AktoerId.class))).thenReturn(Try.of(() -> fnr));

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
