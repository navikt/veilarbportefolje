package no.nav.pto.veilarbportefolje.cv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.common.featuretoggle.UnleashService;
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

    public void behandleKafkaMelding(CVMelding melding) {
        log.info("CV melding pa bruker: {}", melding.getAktoerId());
        AktorId aktoerId = melding.getAktoerId();
        boolean harDeltCv = false;

        if (melding.getRessurs() != CV_HJEMMEL) {
            log.info("Ignorer melding for ressurs {} for bruker {}", melding.getRessurs(), aktoerId);
            return;
        }

        if (melding.getSlettetDato() == null) {
            harDeltCv = true;
        }

        if (erPostgresPa(unleashService)) {
            cvRepositoryV2.upsert(aktoerId, harDeltCv);
        }
        cvRepository.upsert(aktoerId, harDeltCv);
        elasticServiceV2.updateHarDeltCv(aktoerId, harDeltCv);
    }

}
