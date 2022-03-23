package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkjermingPersonerService extends KafkaCommonConsumerService<SkjermingDTO> {
    @Override
    protected void behandleKafkaMeldingLogikk(SkjermingDTO skjermingDTO) {
        LocalDateTime skjermetFra = null;
        LocalDateTime skjermetTil = null;
        if (skjermingDTO.getSkjermetFra() != null && skjermingDTO.getSkjermetFra().length == 6) {
            skjermetFra = LocalDateTime.of(skjermingDTO.getSkjermetFra()[0], skjermingDTO.getSkjermetFra()[1], skjermingDTO.getSkjermetFra()[2], skjermingDTO.getSkjermetFra()[3], skjermingDTO.getSkjermetFra()[4], skjermingDTO.getSkjermetFra()[5]);
        }
        if (skjermingDTO.getSkjermetTil() != null && skjermingDTO.getSkjermetTil().length == 6) {
            skjermetTil = LocalDateTime.of(skjermingDTO.getSkjermetTil()[0], skjermingDTO.getSkjermetTil()[1], skjermingDTO.getSkjermetTil()[2], skjermingDTO.getSkjermetTil()[3], skjermingDTO.getSkjermetTil()[4], skjermingDTO.getSkjermetTil()[5]);
        }

        log.info("Skjerming personer, fra: " + skjermetFra + ", til: " + skjermetTil);
    }
}
