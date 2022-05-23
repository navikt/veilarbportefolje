package no.nav.pto.veilarbportefolje.cv;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import no.nav.pto.veilarbportefolje.cv.dto.Ressurs;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class CvServiceTest extends EndToEndTest {

    @Autowired
    private CVRepositoryV2 cvRepository;

    @Autowired
    private CVService cvService;

    @Autowired
    private JdbcTemplate oracle;

    private final AktorId aktoerId = AktorId.of("00000000000");

    @BeforeEach
    void set_under_oppfolging(){
        oracle.update("truncate TABLE OPPFOLGING_DATA");
        oracle.update("truncate TABLE OPPFOLGINGSBRUKER");
        oracle.update("truncate TABLE AKTOERID_TO_PERSONID");
    }

    @Test
    void skal_hente_fnr_fra_aktoertjenesten_om_fnr_mangler_i_melding() {
        testDataClient.setupBruker(aktoerId, ZonedDateTime.now());
        String document = new JSONObject()
                .put("aktoer_id", aktoerId.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = opensearchTestClient.createDocument(aktoerId, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(200);

        CVMelding cvMelding = new CVMelding();
        cvMelding.setAktoerId(aktoerId);
        cvMelding.setRessurs(Ressurs.CV_HJEMMEL);

        cvService.behandleCVHjemmelMelding(cvMelding);

        GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isTrue();
    }

    @Test
    void skal_oppdatere_dokumentet_i_db_og_opensearch() {
        testDataClient.setupBruker(aktoerId, ZonedDateTime.now());
        String document = new JSONObject()
                .put("aktoer_id", aktoerId.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = opensearchTestClient.createDocument(aktoerId, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(200);

        CVMelding cvMelding = new CVMelding();
        cvMelding.setAktoerId(aktoerId);
        cvMelding.setRessurs(Ressurs.CV_HJEMMEL);

        cvService.behandleCVHjemmelMelding(cvMelding);

        Boolean harDeltCvDb = cvRepository.harDeltCv(aktoerId);
        assertThat(harDeltCvDb).isTrue();

        GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isTrue();
    }

    @Test
    void skal_ikke_behandle_meldinger_som_har_meldingstype_arbeidsgiver_generell() {
        testDataClient.setupBruker(aktoerId, ZonedDateTime.now());
        String document = new JSONObject()
                .put("aktoer_id", aktoerId.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = opensearchTestClient.createDocument(aktoerId, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(200);

        CVMelding cvMelding = new CVMelding();
        cvMelding.setAktoerId(aktoerId);
        cvMelding.setRessurs(Ressurs.ARBEIDSGIVER_GENERELL);

        cvService.behandleCVHjemmelMelding(cvMelding);

        GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isFalse();
    }

    @Test
    void skal_ikke_behandle_meldinger_som_har_meldingstype_cv_generell() {
        testDataClient.setupBruker(aktoerId, ZonedDateTime.now());
        String document = new JSONObject()
                .put("aktoer_id", aktoerId.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = opensearchTestClient.createDocument(aktoerId, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(200);

        CVMelding cvMelding = new CVMelding();
        cvMelding.setAktoerId(aktoerId);
        cvMelding.setRessurs(Ressurs.CV_GENERELL);

        cvService.behandleCVHjemmelMelding(cvMelding);

        GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isFalse();
    }

    @Test
    void skal_ignorere_tilfeller_hvor_dokumentet_ikke_finnes_i_opensearch() {
        CVMelding cvMelding = new CVMelding();
        cvMelding.setAktoerId(aktoerId);
        cvMelding.setRessurs(Ressurs.CV_HJEMMEL);

        cvService.behandleCVHjemmelMelding(cvMelding);

        GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isFalse();
    }
}
