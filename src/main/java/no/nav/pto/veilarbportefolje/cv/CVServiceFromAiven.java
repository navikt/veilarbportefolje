package no.nav.pto.veilarbportefolje.cv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erPostgresPa;
import static no.nav.pto.veilarbportefolje.cv.dto.Ressurs.CV_HJEMMEL;

@RequiredArgsConstructor
@Service
@Slf4j
public class CVServiceFromAiven {
    private final ElasticServiceV2 elasticServiceV2;
    private final CvRepository cvRepository;
    private final CVRepositoryV2 cvRepositoryV2;
    private final UnleashService unleashService;

    public void behandleKafkaMelding(ConsumerRecord<String, CVMelding> kafkaMelding) {
        CVMelding cvMelding = kafkaMelding.value();
        behandleCVMelding(cvMelding);
    }

    public void behandleCVMelding(CVMelding cvMelding) {
        log.info("CV melding pa bruker: {}", cvMelding.getAktoerId());
        AktorId aktoerId = cvMelding.getAktoerId();
        boolean harDeltCv = false;

        if (cvMelding.getRessurs() != CV_HJEMMEL) {
            log.info("Ignorer melding for ressurs {} for bruker {}", cvMelding.getRessurs(), aktoerId);
            return;
        }

        if (cvMelding.getSlettetDato() == null) {
            harDeltCv = true;
        }

        if (erPostgresPa(unleashService)) {
            cvRepositoryV2.upsert(aktoerId, harDeltCv);
        }
        cvRepository.upsert(aktoerId, harDeltCv);
        elasticServiceV2.updateHarDeltCv(aktoerId, harDeltCv);
    }
}
