package no.nav.pto.veilarbportefolje.kafka;

import no.nav.arbeid.cv.avro.Melding;
import no.nav.arbeid.cv.avro.Meldingstype;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.CVService;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import no.nav.pto.veilarbportefolje.cv.dto.Ressurs;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.elasticsearch.action.get.GetResponse;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutionException;

import static java.util.Arrays.stream;
import static no.nav.pto.veilarbportefolje.util.ElasticTestClient.pollElasticUntil;

class CvServiceKafkaConsumerTest extends EndToEndTest {

    @Autowired
    private CVService cvService;

    @Test
    void testCVHjemmel() throws ExecutionException, InterruptedException {

        AktorId aktoerId1 = AktorId.of("11111111111");
        AktorId aktoerId2 = AktorId.of("22222222222");
        AktorId aktoerId3 = AktorId.of("33333333333");

        createCvDocumentsInElastic(aktoerId1, aktoerId2, aktoerId3);
        assertHarDeltCVAreFalseInElastic(aktoerId1, aktoerId2, aktoerId3);

        populateCVHjemmelKafkaTopic(aktoerId1, aktoerId2, aktoerId3);
        pollElasticUntil(() -> harDeltCv(aktoerId1, aktoerId2, aktoerId3));
        assertHarDeltCVAreTrueInElastic(aktoerId1, aktoerId2, aktoerId3);
    }

    @Test
    void testCVEksistere() throws ExecutionException, InterruptedException {

        AktorId aktoerId1 = AktorId.of("11111111111");
        AktorId aktoerId2 = AktorId.of("22222222222");
        AktorId aktoerId3 = AktorId.of("33333333333");

        createCvDocumentsInElastic(aktoerId1, aktoerId2, aktoerId3);
        assertCvEksistereAreFalseInElastic(aktoerId1, aktoerId2, aktoerId3);

        populateCVEksistereKafkaTopic(aktoerId1, aktoerId2, aktoerId3);
        pollElasticUntil(() -> hvisCvEksistere(aktoerId1, aktoerId2, aktoerId3));
        assertCvEksistereAreTrueInElastic(aktoerId1, aktoerId2, aktoerId3);
    }

    private boolean harDeltCv(AktorId... aktoerIds) {
        return stream(aktoerIds)
                .map(aktoerId -> elasticTestClient.fetchDocument(aktoerId))
                .allMatch(CvServiceKafkaConsumerTest::harDeltCv);
    }

    private boolean hvisCvEksistere(AktorId... aktoerIds) {
        return stream(aktoerIds)
                .map(aktoerId -> elasticTestClient.fetchDocument(aktoerId))
                .allMatch(CvServiceKafkaConsumerTest::cvEksistere);
    }

    private void createCvDocumentsInElastic(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            createCvDocument(aktoerId);
        }
    }

    private void assertHarDeltCVAreTrueInElastic(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
            Assert.assertTrue(harDeltCv(getResponse));
        }
    }

    private void assertHarDeltCVAreFalseInElastic(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
            Assert.assertFalse(harDeltCv(getResponse));
        }
    }

    private void assertCvEksistereAreTrueInElastic(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
            Assert.assertTrue(cvEksistere(getResponse));
        }
    }

    private void assertCvEksistereAreFalseInElastic(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
            Assert.assertFalse(cvEksistere(getResponse));
        }
    }

    private static boolean harDeltCv(GetResponse get1) {
        return (boolean) get1.getSourceAsMap().get("har_delt_cv");
    }

    private static boolean cvEksistere(GetResponse get1) {
        return (boolean) get1.getSourceAsMap().get("cv_eksistere");
    }

    private void createCvDocument(AktorId aktoerId) {
        String document = new JSONObject()
                .put("aktoer_id", aktoerId.get())
                .put("har_delt_cv", false)
                .put("cv_eksistere", false)
                .toString();

        elasticTestClient.createDocument(aktoerId, document);
    }

    private void populateCVHjemmelKafkaTopic(AktorId... aktoerIds) throws ExecutionException, InterruptedException {
        for (AktorId aktoerId : aktoerIds) {
            CVMelding cvMelding = new CVMelding();
            cvMelding.setAktoerId(aktoerId);
            cvMelding.setRessurs(Ressurs.CV_HJEMMEL);

            cvService.behandleCVHjemmelMelding(cvMelding);
        }
    }

    private void populateCVEksistereKafkaTopic(AktorId... aktoerIds) throws ExecutionException, InterruptedException {
        for (AktorId aktoerId : aktoerIds) {
            Melding cvMelding = new Melding();
            cvMelding.setAktoerId(aktoerId.toString());
            cvMelding.setMeldingstype(Meldingstype.ENDRE);

            cvService.behandleKafkaRecord(new ConsumerRecord("test topic", 0, 0, 0, cvMelding));
        }
    }
}
