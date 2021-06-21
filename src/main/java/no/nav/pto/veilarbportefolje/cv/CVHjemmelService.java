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


@RequiredArgsConstructor
@Service
@Slf4j
public class CVHjemmelService implements KafkaConsumerService<Melding> {
    private final ElasticServiceV2 elasticServiceV2;
    private final CvRepository cvRepository;
    private final AtomicBoolean rewind = new AtomicBoolean(false);

    private boolean cvEksistere(Melding melding) {
        return melding.getMeldingstype() == Meldingstype.ENDRE || melding.getMeldingstype() == Meldingstype.OPPRETT;
    }

    @Override
    public void behandleKafkaMelding(Melding kafkaMelding) {
        log.info(
                "Behandler kafka-melding med key {} på topic {}",
                kafkaMelding.getAktoerId(),
                KafkaConfig.Topic.CV_ENDRET.getTopicName()
        );
        AktorId aktoerId = AktorId.of(kafkaMelding.getAktoerId());

        boolean cvEksisterer = cvEksistere(kafkaMelding);
        cvRepository.upsertCvEksistere(aktoerId, cvEksisterer);
        elasticServiceV2.updateCvEksistere(aktoerId, cvEksisterer);
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
