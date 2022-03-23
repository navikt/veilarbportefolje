package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkjermingPersonerService extends KafkaCommonConsumerService<SkjermingDTO> {
    @Override
    protected void behandleKafkaMeldingLogikk(SkjermingDTO skjermingDTO) {
        log.info("Skjerming personer, fra: " + skjermingDTO.getSkjermetFra() + ", til: " + skjermingDTO.getSkjermetTil());
    }
}
