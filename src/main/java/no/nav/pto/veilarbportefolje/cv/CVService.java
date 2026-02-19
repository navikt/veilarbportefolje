package no.nav.pto.veilarbportefolje.cv;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.cv.avro.Melding;
import no.nav.arbeid.cv.avro.Meldingstype;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonNonKeyedConsumerService;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerPaDatafelt;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@RequiredArgsConstructor
@Service
@Slf4j
public class CVService extends KafkaCommonNonKeyedConsumerService<Melding> {
    private final OpensearchIndexerPaDatafelt opensearchIndexerPaDatafelt;
    private final CVRepositoryV2 cvRepositoryV2;

    @Override
    public void behandleKafkaMeldingLogikk(Melding kafkaMelding) {
        AktorId aktoerId = AktorId.of(String.valueOf(kafkaMelding.getAktoerId()));
        boolean cvEksisterer = cvEksistere(kafkaMelding);

        if (cvEksisterer) {
            secureLog.info("Oppdater CV eksisterer for bruker: {}", aktoerId.get());
            cvRepositoryV2.upsertCVEksisterer(aktoerId, true);
            opensearchIndexerPaDatafelt.updateCvEksistere(aktoerId, cvEksisterer);
        } else {
            secureLog.info("Slett CV eksisterer for bruker: {}", aktoerId.get());
            cvRepositoryV2.slettCvEksisterer(aktoerId);
            opensearchIndexerPaDatafelt.slettCV(aktoerId);
        }
    }

    private boolean cvEksistere(Melding melding) {
        return melding.getMeldingstype() == Meldingstype.ENDRE || melding.getMeldingstype() == Meldingstype.OPPRETT;
    }
}
