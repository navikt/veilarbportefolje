package no.nav.pto.veilarbportefolje.kafka;

import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.elasticsearch.action.get.GetResponse;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ExecutionException;

import static java.util.Arrays.stream;
import static no.nav.pto.veilarbportefolje.kafka.KafkaConfig.Topic.PAM_SAMTYKKE_ENDRET_V1;
import static no.nav.pto.veilarbportefolje.util.ElasticTestClient.pollElasticUntil;
import static org.assertj.core.api.Assertions.assertThat;

class CvKafkaConsumerTest extends EndToEndTest {

    @Autowired
    private CvService cvService;

    @Autowired
    private KafkaProducer<String, String> kafkaProducer;

    @Test
    void skal_populere_elastic_med_cv_og_spole_tilbake() throws ExecutionException, InterruptedException {

        AktoerId aktoerId1 = AktoerId.of("11111111111");
        AktoerId aktoerId2 = AktoerId.of("22222222222");
        AktoerId aktoerId3 = AktoerId.of("33333333333");

        createCvDocumentsInElastic(aktoerId1, aktoerId2, aktoerId3);
        assertCvDocumentsAreFalseInElastic(aktoerId1, aktoerId2, aktoerId3);

        populateKafkaTopic(aktoerId1, aktoerId2, aktoerId3);
        pollElasticUntil(() -> harDeltCv(aktoerId1, aktoerId2, aktoerId3));
        assertCvDocumentsAreTrueInElastic(aktoerId1, aktoerId2, aktoerId3);

        createCvDocumentsInElastic(aktoerId1, aktoerId2, aktoerId3);
        assertCvDocumentsAreFalseInElastic(aktoerId1, aktoerId2, aktoerId3);

        cvService.setRewind(true);
        pollElasticUntil(() -> harDeltCv(aktoerId1, aktoerId2, aktoerId3));
        assertCvDocumentsAreTrueInElastic(aktoerId1, aktoerId2, aktoerId3);

    }

    private void assertCvDocumentsAreTrueInElastic(AktoerId... aktoerIds) {
        for (AktoerId aktoerId : aktoerIds) {
            GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
            assertThat(harDeltCv(getResponse)).isTrue();
        }
    }


    private boolean harDeltCv(AktoerId... aktoerIds) {
        return stream(aktoerIds)
                .map(aktoerId -> elasticTestClient.fetchDocument(aktoerId))
                .allMatch(CvKafkaConsumerTest::harDeltCv);
    }

    private void createCvDocumentsInElastic(AktoerId... aktoerIds) {
        for (AktoerId aktoerId : aktoerIds) {
            createCvDocument(aktoerId);
        }
    }

    private void assertCvDocumentsAreFalseInElastic(AktoerId... aktoerIds) {
        for (AktoerId aktoerId : aktoerIds) {
            GetResponse getResponse = elasticTestClient.fetchDocument(aktoerId);
            assertThat(harDeltCv(getResponse)).isFalse();
        }
    }

    private static boolean harDeltCv(GetResponse get1) {
        return (boolean) get1.getSourceAsMap().get("har_delt_cv");
    }

    private void createCvDocument(AktoerId aktoerId) {
        String document = new JSONObject()
                .put("aktoer_id", aktoerId.toString())
                .put("har_delt_cv", false)
                .toString();

        elasticTestClient.createDocument(aktoerId, document);
    }

    private void populateKafkaTopic(AktoerId... aktoerIds) throws ExecutionException, InterruptedException {
        for (AktoerId aktoerId : aktoerIds) {
            String payload = new JSONObject()
                    .put("aktoerId", aktoerId.toString())
                    .put("meldingType", "SAMTYKKE_OPPRETTET")
                    .put("ressurs", "CV_HJEMMEL")
                    .toString();

            ProducerRecord<String, String> record = new ProducerRecord<>(PAM_SAMTYKKE_ENDRET_V1.topicName, aktoerId.toString(), payload);
            kafkaProducer.send(record).get();
        }
    }
}
