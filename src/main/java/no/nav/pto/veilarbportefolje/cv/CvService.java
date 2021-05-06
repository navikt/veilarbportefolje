package no.nav.pto.veilarbportefolje.cv;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.cv.dto.MeldingType;
import no.nav.pto.veilarbportefolje.cv.dto.Ressurs;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.cv.dto.Ressurs.CV_HJEMMEL;

@Slf4j
@Service
public class CvService implements KafkaConsumerService<String> {
    private final ElasticServiceV2 elasticServiceV2;
    private final CvRepository cvRepository;

    private final AtomicBoolean rewind;

    @Value
    static class Melding {
        AktorId aktoerId;
        Fnr fnr;
        MeldingType meldingType;
        Ressurs ressurs;
    }

    @Autowired
    public CvService(ElasticServiceV2 elasticServiceV2, CvRepository cvRepository) {
        this.elasticServiceV2 = elasticServiceV2;
        this.cvRepository = cvRepository;
        this.rewind = new AtomicBoolean();
    }

    @Override
    public boolean shouldRewind() {
        return rewind.get();
    }

    @Override
    public void setRewind(boolean rewind) {
        this.rewind.set(rewind);
    }

    @Override
    public void behandleKafkaMelding(String payload) {
        Melding melding = fromJson(payload, Melding.class);
        AktorId aktoerId = melding.getAktoerId();

        if (melding.getRessurs() != CV_HJEMMEL) {
            log.info("Ignorer melding for ressurs {} for bruker {}", melding.getRessurs(), aktoerId);
            return;
        }

        switch (melding.meldingType) {
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

    public void setCVSamtykke(AktorId aktoerId) {
        cvRepository.upsert(aktoerId, true);
        elasticServiceV2.updateHarDeltCv(aktoerId, true);
    }
}
