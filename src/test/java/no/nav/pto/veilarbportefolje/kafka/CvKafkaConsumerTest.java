package no.nav.pto.veilarbportefolje.kafka;

import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.cv.CvRepository;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.domene.Fnr;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.elastic.domene.ElasticIndex;
import org.elasticsearch.action.get.GetResponse;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.util.Arrays.stream;
import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.pto.veilarbportefolje.TestUtil.createUnleashMock;
import static no.nav.pto.veilarbportefolje.kafka.KafkaConfig.Topic.PAM_SAMTYKKE_ENDRET_V1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class CvKafkaConsumerTest extends IntegrationTest {
    private static CvService cvService;
    private static String indexName;

    @BeforeClass
    public static void beforeClass() {
        indexName = generateId();
        cvService = new CvService(new ElasticServiceV2(ELASTIC_CLIENT, ElasticIndex.of(indexName)), mock(AktorregisterClient.class), mock(CvRepository.class));

        new KafkaConsumerRunnable<>(
                cvService,
                createUnleashMock(),
                getKafkaConsumerProperties(),
                PAM_SAMTYKKE_ENDRET_V1,
                mock(MetricsClient.class)
        );
    }

    @Test
    public void skal_populere_elastic_med_cv_og_spole_tilbake() {

        Fnr fnr1 = Fnr.of("11111111111");
        Fnr fnr2 = Fnr.of("22222222222");
        Fnr fnr3 = Fnr.of("33333333333");

        createIndex(indexName);

        createCvDocumentsInElastic(fnr1, fnr2, fnr3);
        assertCvDocumentsAreFalseInElastic(fnr1, fnr2, fnr3);

        populateKafkaTopic(fnr1, fnr2, fnr3);
        pollUntilHarOppdatertIElastic(() -> untilHarDeltCvIsTrue(fnr1, fnr2, fnr3));

        assertCvDocumentsAreTrueInElastic(fnr1, fnr2, fnr3);

        deleteIndex(indexName);

        createCvDocumentsInElastic(fnr1, fnr2, fnr3);
        assertCvDocumentsAreFalseInElastic(fnr1, fnr2, fnr3);

        cvService.setRewind(true);
        pollUntilHarOppdatertIElastic(()-> untilHarDeltCvIsTrue(fnr1, fnr2, fnr3));
        assertCvDocumentsAreTrueInElastic(fnr1, fnr2, fnr3);

    }

    private void assertCvDocumentsAreTrueInElastic(Fnr... fnrs) {
        for (Fnr fnr : fnrs) {
            GetResponse getResponse = fetchDocument(indexName, fnr);
            assertThat(harDeltCv(getResponse)).isTrue();
        }
    }


    private boolean untilHarDeltCvIsTrue(Fnr... fnrs) {
        return !stream(fnrs)
                .map(fnr -> fetchDocument(indexName, fnr))
                .allMatch(CvKafkaConsumerTest::harDeltCv);
    }

    private void createCvDocumentsInElastic(Fnr... fnrs) {
        for (Fnr fnr : fnrs) {
            createCvDocument(fnr);
        }
    }

    private void assertCvDocumentsAreFalseInElastic(Fnr... fnrs) {
        for (Fnr fnr : fnrs) {
            GetResponse getResponse = fetchDocument(indexName, fnr);
            assertThat(harDeltCv(getResponse)).isFalse();
        }
    }

    private static boolean harDeltCv(GetResponse get1) {
        return (boolean) get1.getSourceAsMap().get("har_delt_cv");
    }

    private void createCvDocument(Fnr fnr) {
        String document = new JSONObject()
                .put("fnr", fnr.toString())
                .put("har_delt_cv", false)
                .toString();

        createDocument(indexName, fnr, document);
    }

    private static void populateKafkaTopic(Fnr... fnrs) {
        for (Fnr fnr : fnrs) {
            String aktoerId = generateId();
            String payload = new JSONObject()
                    .put("aktoerId", aktoerId)
                    .put("fnr", fnr.toString())
                    .put("meldingType", "SAMTYKKE_OPPRETTET")
                    .put("ressurs", "CV_HJEMMEL")
                    .toString();

            populateKafkaTopic(PAM_SAMTYKKE_ENDRET_V1.topic, aktoerId, payload);

        };
    }
}
