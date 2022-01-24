package no.nav.pto.veilarbportefolje.kafka;

import no.nav.arbeid.cv.avro.Melding;
import no.nav.arbeid.cv.avro.Meldingstype;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.CVService;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import no.nav.pto.veilarbportefolje.cv.dto.Ressurs;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.action.get.GetResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZonedDateTime;
import java.util.concurrent.ExecutionException;

import static java.util.Arrays.stream;
import static no.nav.pto.veilarbportefolje.util.OpensearchTestClient.pollOpensearchUntil;

class CvServiceKafkaConsumerTest extends EndToEndTest {

    @Autowired
    private CVService cvService;

    @Autowired
    private OppfolgingRepository oppfolgingRepository;

    @Autowired
    private OppfolgingRepositoryV2 oppfolgingRepositoryV2;


    private final AktorId aktoerId1 = AktorId.of("11111111111");
    private final AktorId aktoerId2 = AktorId.of("22222222222");
    private final AktorId aktoerId3 = AktorId.of("33333333333");

    @BeforeEach
    void set_under_oppfolging(){
        oppfolgingRepository.settUnderOppfolging(aktoerId1, ZonedDateTime.now());
        oppfolgingRepository.settUnderOppfolging(aktoerId2, ZonedDateTime.now());
        oppfolgingRepository.settUnderOppfolging(aktoerId3, ZonedDateTime.now());
        oppfolgingRepositoryV2.settUnderOppfolging(aktoerId1, ZonedDateTime.now());
        oppfolgingRepositoryV2.settUnderOppfolging(aktoerId2, ZonedDateTime.now());
        oppfolgingRepositoryV2.settUnderOppfolging(aktoerId3, ZonedDateTime.now());
    }

    @Test
    void testCVHjemmel() throws ExecutionException, InterruptedException {
        createCvDocumentsInOpensearch(aktoerId1, aktoerId2, aktoerId3);
        assertHarDeltCVAreFalseInOpensearch(aktoerId1, aktoerId2, aktoerId3);

        populateCVHjemmelKafkaTopic(aktoerId1, aktoerId2, aktoerId3);
        pollOpensearchUntil(() -> harDeltCv(aktoerId1, aktoerId2, aktoerId3));
        assertHarDeltCVAreTrueInOpensearch(aktoerId1, aktoerId2, aktoerId3);
    }

    @Test
    void testCVEksistere() throws ExecutionException, InterruptedException {
        createCvDocumentsInOpensearch(aktoerId1, aktoerId2, aktoerId3);
        assertCvEksistereAreFalseInOpensearch(aktoerId1, aktoerId2, aktoerId3);

        populateCVEksistereKafkaTopic(aktoerId1, aktoerId2, aktoerId3);
        pollOpensearchUntil(() -> hvisCvEksistere(aktoerId1, aktoerId2, aktoerId3));
        assertCvEksistereAreTrueInOpensearch(aktoerId1, aktoerId2, aktoerId3);
    }

    private boolean harDeltCv(AktorId... aktoerIds) {
        return stream(aktoerIds)
                .map(aktoerId -> opensearchTestClient.fetchDocument(aktoerId))
                .allMatch(CvServiceKafkaConsumerTest::harDeltCv);
    }

    private boolean hvisCvEksistere(AktorId... aktoerIds) {
        return stream(aktoerIds)
                .map(aktoerId -> opensearchTestClient.fetchDocument(aktoerId))
                .allMatch(CvServiceKafkaConsumerTest::cvEksistere);
    }

    private void createCvDocumentsInOpensearch(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            createCvDocument(aktoerId);
        }
    }

    private void assertHarDeltCVAreTrueInOpensearch(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);
            Assertions.assertTrue(harDeltCv(getResponse));
        }
    }

    private void assertHarDeltCVAreFalseInOpensearch(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);
            Assertions.assertFalse(harDeltCv(getResponse));
        }
    }

    private void assertCvEksistereAreTrueInOpensearch(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);
            Assertions.assertTrue(cvEksistere(getResponse));
        }
    }

    private void assertCvEksistereAreFalseInOpensearch(AktorId... aktoerIds) {
        for (AktorId aktoerId : aktoerIds) {
            GetResponse getResponse = opensearchTestClient.fetchDocument(aktoerId);
            Assertions.assertFalse(cvEksistere(getResponse));
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

        opensearchTestClient.createDocument(aktoerId, document);
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

            cvService.behandleKafkaMeldingLogikk(cvMelding);
        }
    }
}
