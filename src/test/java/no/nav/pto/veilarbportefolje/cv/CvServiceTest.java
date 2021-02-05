package no.nav.pto.veilarbportefolje.cv;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
class CvServiceTest extends EndToEndTest {

    @Autowired
    private CvRepository cvRepository;

    @Autowired
    private CvService cvService;

    @Test
    void skal_hente_fnr_fra_aktoertjenesten_om_fnr_mangler_i_melding() {
        AktorId aktoerId = AktorId.of("00000000000");

        String document = new JSONObject()
                .put("aktoer_id", aktoerId.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = elasticTestClient.createDocument(aktoerId, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(201);

        String payload = new JSONObject()
                .put("aktoerId", "00000000000")
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "CV_HJEMMEL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isTrue();
    }

    @Test
    void skal_oppdatere_dokumentet_i_db_og_elastic() {
        AktorId aktoerId = AktorId.of("00000000000");

        String document = new JSONObject()
                .put("aktoer_id", aktoerId.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = elasticTestClient.createDocument(aktoerId, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(201);

        String payload = new JSONObject()
                .put("aktoerId", aktoerId.toString())
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "CV_HJEMMEL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        String harDeltCvDb = cvRepository.harDeltCv(aktoerId);
        assertThat(harDeltCvDb).isEqualTo("J");

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isTrue();
    }

    @Test
    void skal_ikke_behandle_meldinger_som_har_meldingstype_arbeidsgiver_generell() {
        AktorId aktoerId = AktorId.of("00000000000");

        String document = new JSONObject()
                .put("aktoer_id", aktoerId.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = elasticTestClient.createDocument(aktoerId, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(201);

        String payload = new JSONObject()
                .put("aktoerId", "00000000000")
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "ARBEIDSGIVER_GENERELL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isFalse();
    }

    @Test
    void skal_ikke_behandle_meldinger_som_har_meldingstype_cv_generell() {
        AktorId aktoerId = AktorId.of("00000000000");
        String document = new JSONObject()
                .put("aktoer_id", aktoerId.toString())
                .put("har_delt_cv", false)
                .toString();

        IndexResponse indexResponse = elasticTestClient.createDocument(aktoerId, document);
        assertThat(indexResponse.status().getStatus()).isEqualTo(201);

        String payload = new JSONObject()
                .put("aktoerId", aktoerId.toString())
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "CV_GENERELL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isTrue();

        boolean harDeltCv = (boolean) getResponse.getSourceAsMap().get("har_delt_cv");
        assertThat(harDeltCv).isFalse();
    }

    @Test
    void skal_ignorere_tilfeller_hvor_dokumentet_ikke_finnes_i_elastic() {
        AktorId aktoerId = AktorId.of("00000000000");
        String payload = new JSONObject()
                .put("aktoerId", aktoerId)
                .put("meldingType", "SAMTYKKE_OPPRETTET")
                .put("ressurs", "CV_HJEMMEL")
                .toString();

        cvService.behandleKafkaMelding(payload);

        GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
        assertThat(getResponse.isExists()).isFalse();
    }
}
