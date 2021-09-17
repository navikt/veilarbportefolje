package no.nav.pto.veilarbportefolje.cv;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.cv.avro.Melding;
import no.nav.arbeid.cv.avro.Meldingstype;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erPostgresPa;
import static no.nav.pto.veilarbportefolje.cv.dto.Ressurs.CV_HJEMMEL;


@RequiredArgsConstructor
@Service
@Slf4j
public class CVService extends KafkaCommonConsumerService<Melding> implements KafkaConsumerService<Melding> {
    private final ElasticServiceV2 elasticServiceV2;
    private final CvRepository cvRepository;
    private final OppfolgingRepository oppfolgingRepository;
    private final AtomicBoolean rewind = new AtomicBoolean(false);
    private final CVRepositoryV2 cvRepositoryV2;
    @Getter
    private final UnleashService unleashService;

    @Override
    public void behandleKafkaMelding(Melding kafkaMelding) {
        behandleKafkaMeldingLogikk(kafkaMelding);
    }

    @Override
    protected void behandleKafkaMeldingLogikk(Melding kafkaMelding) {
        AktorId aktoerId = AktorId.of(kafkaMelding.getAktoerId());

        boolean cvEksisterer = cvEksistere(kafkaMelding);
        if (erPostgresPa(unleashService)) {
            cvRepositoryV2.upsertCVEksisterer(aktoerId, cvEksisterer);
        }
        cvRepository.upsertCvEksistere(aktoerId, cvEksisterer);
        //elasticServiceV2.updateCvEksistere(aktoerId, cvEksisterer);
    }

    public void behandleKafkaMeldingCVHjemmel(ConsumerRecord<String, CVMelding> kafkaMelding) {
        log.info(
                "Behandler kafka-melding med key {} og offset {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.topic()
        );
        CVMelding cvMelding = kafkaMelding.value();
        behandleCVHjemmelMelding(cvMelding);
    }

    public void behandleCVHjemmelMelding(CVMelding cvMelding) {
        AktorId aktoerId = cvMelding.getAktoerId();
        boolean harDeltCv = (cvMelding.getSlettetDato() == null);

        if (cvMelding.getRessurs() != CV_HJEMMEL) {
            log.info("Ignorer melding for ressurs {} for bruker {}", cvMelding.getRessurs(), aktoerId);
            return;
        }

        if (erPostgresPa(unleashService)) {
            cvRepositoryV2.upsertHarDeltCv(aktoerId, harDeltCv);
        }
        cvRepository.upsertHarDeltCv(aktoerId, harDeltCv);
        elasticServiceV2.updateHarDeltCv(aktoerId, harDeltCv);

        if (!oppfolgingRepository.erUnderoppfolging(cvMelding.getAktoerId())) {
            elasticServiceV2.deleteIfPresent(cvMelding.getAktoerId(),
                    String.format("(CvService) Sletter aktorId da brukeren ikke lengre er under oppfolging %s", cvMelding.getAktoerId()));
        }
    }

    private boolean cvEksistere(Melding melding) {
        return melding.getMeldingstype() == Meldingstype.ENDRE || melding.getMeldingstype() == Meldingstype.OPPRETT;
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
