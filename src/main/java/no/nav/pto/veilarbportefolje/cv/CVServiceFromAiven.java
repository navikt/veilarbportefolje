package no.nav.pto.veilarbportefolje.cv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.cv.dto.Ressurs.CV_HJEMMEL;


@RequiredArgsConstructor
@Service
@Slf4j
public class CVServiceFromAiven {
    private final ElasticServiceV2 elasticServiceV2;
    private final CvRepository cvRepository;

    public void behandleKafkaMelding(CVMelding melding) {
        AktorId aktoerId = melding.getAktoerId();

        if (melding.getRessurs() != CV_HJEMMEL) {
            log.info("Ignorer melding for ressurs {} for bruker {}", melding.getRessurs(), aktoerId);
            return;
        }

        switch (melding.getMeldingType()) {
            case SAMTYKKE_OPPRETTET:
                cvRepository.upsert(aktoerId, true);
                elasticServiceV2.updateHarDeltCv(aktoerId, true);
                break;
            case SAMTYKKE_SLETTET:
                cvRepository.upsert(aktoerId, false);
                elasticServiceV2.updateHarDeltCv(aktoerId, false);
                break;
            default:
                log.info("Ignorer melding av type {} for bruker {}", melding.getMeldingType(), aktoerId);
        }
    }
}
