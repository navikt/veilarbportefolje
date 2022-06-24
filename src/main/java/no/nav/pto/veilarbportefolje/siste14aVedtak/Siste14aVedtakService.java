package no.nav.pto.veilarbportefolje.siste14aVedtak;

import lombok.RequiredArgsConstructor;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Siste14aVedtakService extends KafkaCommonConsumerService<Siste14aVedtakDTO> {

    Siste14aVedtakRepository siste14aVedtakRepository;

    @Override
    protected void behandleKafkaMeldingLogikk(Siste14aVedtakDTO kafkaMelding) {
        siste14aVedtakRepository.upsert(kafkaMelding);
    }

}
