package no.nav.pto.veilarbportefolje.oppfolging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkjermingService extends KafkaCommonConsumerService<String> {

    private static final Base64.Decoder decoder = Base64.getDecoder();

    @Override
    protected void behandleKafkaMeldingLogikk(String payload) {
        String ident = new String(decoder.decode(payload));
        log.info("Skjerming for: " + ident);
    }
}
