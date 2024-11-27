package no.nav.pto.veilarbportefolje.cv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.cv.avro.Melding;
import no.nav.arbeid.cv.avro.Meldingstype;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.cv.dto.Ressurs.CV_HJEMMEL;
import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;


@RequiredArgsConstructor
@Service
@Slf4j
public class CVService extends KafkaCommonNonKeyedConsumerService<Melding> {
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final CVRepositoryV2 cvRepositoryV2;

    @Override
    public void behandleKafkaMeldingLogikk(Melding kafkaMelding) {
        AktorId aktoerId = AktorId.of(String.valueOf(kafkaMelding.getAktoerId()));
        boolean cvEksisterer = cvEksistere(kafkaMelding);
        secureLog.info("Oppdater CV eksisterer for bruker: {}, eksisterer: {}", aktoerId.get(), cvEksisterer);

        cvRepositoryV2.upsertCVEksisterer(aktoerId, cvEksisterer);
        opensearchIndexerV2.updateCvEksistere(aktoerId, cvEksisterer);
    }

    public void behandleKafkaMeldingCVHjemmel(ConsumerRecord<String, CVMelding> kafkaMelding) {
        secureLog.info(
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
            secureLog.info("Ignorer melding for ressurs {} for bruker {}", cvMelding.getRessurs(), aktoerId);
            return;
        }

        secureLog.info("Oppdaterte bruker: {}. Har delt cv: {}", aktoerId, harDeltCv);
        cvRepositoryV2.upsertHarDeltCv(aktoerId, harDeltCv);

        opensearchIndexerV2.updateHarDeltCv(aktoerId, harDeltCv);
    }

    private boolean cvEksistere(Melding melding) {
        return melding.getMeldingstype() == Meldingstype.ENDRE || melding.getMeldingstype() == Meldingstype.OPPRETT;
    }
}
