package no.nav.pto.veilarbportefolje.kafka;

import no.nav.arbeid.cv.avro.Melding;
import no.nav.arbeid.cv.avro.Meldingstype;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.CVHjemmelService;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.elasticsearch.action.get.GetResponse;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutionException;

import static java.util.Arrays.stream;
import static no.nav.pto.veilarbportefolje.util.ElasticTestClient.pollElasticUntil;

class CvEksistereKafkaConsumerTest extends EndToEndTest {

    @Autowired
    private CVHjemmelService cvHjemmelService;

    @Test
    void skal_populere_elastic_med_cv_og_spole_tilbake() throws ExecutionException, InterruptedException {

        AktorId aktoerId1 = AktorId.of("11111111111");
        AktorId aktoerId2 = AktorId.of("22222222222");
        AktorId aktoerId3 = AktorId.of("33333333333");

        createCvDocumentsInElastic(aktoerId1, aktoerId2, aktoerId3);
        assertCvDocumentsAreFalseInElastic(aktoerId1, aktoerId2, aktoerId3);

        populateKafkaTopic(aktoerId1, aktoerId2, aktoerId3);
        pollElasticUntil(() -> hvisCvEksistere(aktoerId1, aktoerId2, aktoerId3));
        assertCvDocumentsAreTrueInElastic(aktoerId1, aktoerId2, aktoerId3);
    }

    private void assertCvDocumentsAreTrueInElastic(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
            Assert.assertTrue(cvEksistere(getResponse));
        }
    }


    private boolean hvisCvEksistere(AktorId... aktoerIds) {
        return stream(aktoerIds)
                .map(aktoerId -> elasticTestClient.fetchDocument(aktoerId))
                .allMatch(CvEksistereKafkaConsumerTest::cvEksistere);
    }

    private void createCvDocumentsInElastic(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            createCvDocument(aktoerId);
        }
    }

    private void assertCvDocumentsAreFalseInElastic(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
            Assert.assertFalse(cvEksistere(getResponse));
        }
    }

    private static boolean cvEksistere(GetResponse get1) {
        return (boolean) get1.getSourceAsMap().get("cv_eksistere");
    }

    private void createCvDocument(AktorId aktoerId) {
        String document = new JSONObject()
                .put("aktoer_id", aktoerId.get())
                .put("cv_eksistere", false)
                .toString();

        elasticTestClient.createDocument(aktoerId, document);
    }

    private void populateKafkaTopic(AktorId... aktoerIds) throws ExecutionException, InterruptedException {
        for (AktorId aktoerId : aktoerIds) {
            Melding cvMelding = new Melding();
            cvMelding.setAktoerId(aktoerId.toString());
            cvMelding.setMeldingstype(Meldingstype.ENDRE);

            cvHjemmelService.behandleKafkaMelding(cvMelding);
        }
    }
}
