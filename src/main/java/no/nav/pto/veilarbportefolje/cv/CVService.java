package no.nav.pto.veilarbportefolje.cv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.cv.avro.Melding;
import no.nav.arbeid.cv.avro.Meldingstype;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.cv.dto.CVMelding;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static no.nav.pto.veilarbportefolje.cv.dto.Ressurs.CV_HJEMMEL;


@RequiredArgsConstructor
@Service
@Slf4j
public class CVService extends KafkaCommonConsumerService<Melding> {
    private final OpensearchIndexerV2 opensearchIndexerV2;
    private final CvRepository cvRepository;
    private final CVRepositoryV2 cvRepositoryV2;

    @Override
    public void behandleKafkaMeldingLogikk(Melding kafkaMelding) {
        AktorId aktoerId = AktorId.of(kafkaMelding.getAktoerId());
        boolean cvEksisterer = cvEksistere(kafkaMelding);
        log.info("On prem: Oppdater CV eksisterer for bruker: {}, eksisterer: {}", aktoerId.get(), cvEksisterer);
        cvRepository.upsertCvEksistere(aktoerId, cvEksisterer);

        opensearchIndexerV2.updateCvEksistere(aktoerId, cvEksisterer);
    }

    public void behandleKafkaMeldingCVAiven(ConsumerRecord<String, Melding> kafkaMelding) {
        log.info(
                "Behandler kafka-melding med key {} og offset {} på topic {}",
                kafkaMelding.key(),
                kafkaMelding.offset(),
                kafkaMelding.topic()
        );
        Melding cvMelding = kafkaMelding.value();
        behandleKafkaMeldingLogikkAiven(cvMelding);
    }

    // TODO: legg til opensearch
    public void behandleKafkaMeldingLogikkAiven(Melding kafkaMelding) {
        AktorId aktoerId = AktorId.of(kafkaMelding.getAktoerId());
        boolean cvEksisterer = cvEksistere(kafkaMelding);
        log.info("Oppdater CV eksisterer for bruker: {}, eksisterer: {}", aktoerId.get(), cvEksisterer);

        cvRepositoryV2.upsertCVEksisterer(aktoerId, cvEksisterer);
        //opensearchIndexerV2.updateCvEksistere(aktoerId, cvEksisterer);
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

        log.info("Oppdaterte bruker: {}. Har delt cv: {}", aktoerId, harDeltCv);
        cvRepositoryV2.upsertHarDeltCv(aktoerId, harDeltCv);
        cvRepository.upsertHarDeltCv(aktoerId, harDeltCv);

        opensearchIndexerV2.updateHarDeltCv(aktoerId, harDeltCv);
    }

    private boolean cvEksistere(Melding melding) {
        return melding.getMeldingstype() == Meldingstype.ENDRE || melding.getMeldingstype() == Meldingstype.OPPRETT;
    }

    public void migrerCVInfo() {
        brukereSomMåFåNyCvEksistererVerdiIPostgres()
                .forEach(bruker ->
                        cvRepositoryV2.upsertCVEksisterer(bruker, true)
                );
        brukereSomMåFåNyHarSettCVHjemmelVerdiIPostgres()
                .forEach(bruker ->
                        cvRepositoryV2.upsertHarDeltCv(bruker, true)
                );
    }

    public List<AktorId> brukereSomMåFåNyCvEksistererVerdiIPostgres(){
        Set<AktorId> alleBrukereSomHarCVPostgres = cvRepositoryV2.hentAlleBrukereSomHarCV();
        List<AktorId> alleBrukereSomHarCvHjemmelOracle = cvRepository.hentAlleBrukereSomHarCV();

        return alleBrukereSomHarCvHjemmelOracle.stream()
                .filter(cvOracle -> !alleBrukereSomHarCVPostgres.contains(cvOracle))
                .toList();
    }

    public List<AktorId> brukereSomMåFåNyHarSettCVHjemmelVerdiIPostgres(){
        Set<AktorId> alleBrukereSomHarSettHjemmelPostgres = cvRepositoryV2.hentAlleBrukereSomHarSettHjemmel();
        List<AktorId> alleBrukereSomHarSettHjemmelOracle = cvRepository.hentAlleBrukereSomHarSettHjemmel();

        return alleBrukereSomHarSettHjemmelOracle.stream()
                .filter(cvOracle -> !alleBrukereSomHarSettHjemmelPostgres.contains(cvOracle))
                .toList();
    }
}
