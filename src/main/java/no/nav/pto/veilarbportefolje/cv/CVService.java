package no.nav.pto.veilarbportefolje.cv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.cv.avro.Melding;
import no.nav.arbeid.cv.avro.Meldingstype;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConfig;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;


@RequiredArgsConstructor
@Service
@Slf4j
public class CVService implements KafkaConsumerService<String> {
    private final ElasticServiceV2 elasticServiceV2;
    private final CvRepository cvRepository;
    private final AtomicBoolean rewind = new AtomicBoolean(false);

    private boolean cvEksistere(Melding melding) {
        return melding.getMeldingstype() == Meldingstype.ENDRE || melding.getMeldingstype() == Meldingstype.OPPRETT;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        Melding melding = fromJson(kafkaMelding, Melding.class);

        log.info("Fikk endring pa cv: {}, topic: {}", KafkaConfig.Topic.CV_ENDRET, melding.getAktoerId());
        AktorId aktoerId = AktorId.of(melding.getAktoerId());

        if (cvEksistere(melding)) {
            cvRepository.upsertCvEksistere(aktoerId, true);
            elasticServiceV2.updateCvEksistere(aktoerId, true);
        } else {
            cvRepository.upsertCvEksistere(aktoerId, false);
            elasticServiceV2.updateCvEksistere(aktoerId, false);
        }
    }

    @Override
    public boolean shouldRewind() {
        return rewind.get();
    }

    @Override
    public void setRewind(boolean rewind) {
        this.rewind.set(rewind);
    }
}
