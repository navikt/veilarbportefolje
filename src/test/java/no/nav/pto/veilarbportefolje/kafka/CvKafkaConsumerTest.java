package no.nav.pto.veilarbportefolje.kafka;

import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.CVServiceFromAiven;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import no.nav.pto.veilarbportefolje.cv.dto.Ressurs;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.elasticsearch.action.get.GetResponse;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutionException;

import static java.util.Arrays.stream;
import static no.nav.pto.veilarbportefolje.util.ElasticTestClient.pollElasticUntil;
import static org.assertj.core.api.Assertions.assertThat;

class CvKafkaConsumerTest extends EndToEndTest {

    @Autowired
    private CVServiceFromAiven cvService;

    @Test
    void skal_populere_elastic_med_cv_og_spole_tilbake() throws ExecutionException, InterruptedException {

        AktorId aktoerId1 = AktorId.of("11111111111");
        AktorId aktoerId2 = AktorId.of("22222222222");
        AktorId aktoerId3 = AktorId.of("33333333333");

        createCvDocumentsInElastic(aktoerId1, aktoerId2, aktoerId3);
        assertCvDocumentsAreFalseInElastic(aktoerId1, aktoerId2, aktoerId3);

        populateKafkaTopic(aktoerId1, aktoerId2, aktoerId3);
        pollElasticUntil(() -> harDeltCv(aktoerId1, aktoerId2, aktoerId3));
        assertCvDocumentsAreTrueInElastic(aktoerId1, aktoerId2, aktoerId3);

        createCvDocumentsInElastic(aktoerId1, aktoerId2, aktoerId3);
        assertCvDocumentsAreFalseInElastic(aktoerId1, aktoerId2, aktoerId3);

        populateKafkaTopic(aktoerId1, aktoerId2, aktoerId3);
        pollElasticUntil(() -> harDeltCv(aktoerId1, aktoerId2, aktoerId3));
        assertCvDocumentsAreTrueInElastic(aktoerId1, aktoerId2, aktoerId3);

    }

    private void assertCvDocumentsAreTrueInElastic(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
            assertThat(harDeltCv(getResponse)).isTrue();
        }
    }


    private boolean harDeltCv(AktorId... aktoerIds) {
        return stream(aktoerIds)
                .map(aktoerId -> elasticTestClient.fetchDocument(aktoerId))
                .allMatch(CvKafkaConsumerTest::harDeltCv);
    }

    private void createCvDocumentsInElastic(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            createCvDocument(aktoerId);
        }
    }

    private void assertCvDocumentsAreFalseInElastic(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
            assertThat(harDeltCv(getResponse)).isFalse();
        }
    }

    private static boolean harDeltCv(GetResponse get1) {
        return (boolean) get1.getSourceAsMap().get("har_delt_cv");
    }

    private void createCvDocument(AktorId aktoerId) {
        String document = new JSONObject()
                .put("aktoer_id", aktoerId.get())
                .put("har_delt_cv", false)
                .toString();

        elasticTestClient.createDocument(aktoerId, document);
    }

    private void populateKafkaTopic(AktorId... aktoerIds) throws ExecutionException, InterruptedException {
        for (AktorId aktoerId : aktoerIds) {
            CVMelding cvMelding = new CVMelding();
            cvMelding.setAktoerId(aktoerId);
            cvMelding.setRessurs(Ressurs.CV_HJEMMEL);

            cvService.behandleCVMelding(cvMelding);
        }
    }
}
