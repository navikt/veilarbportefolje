package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;

@Service
@Slf4j
@RequiredArgsConstructor
public class OppfolginsbrukerService implements KafkaConsumerService<String> {
    private final OppfolginsbrukerRepositoryV2 OppfolginsbrukerRepositoryV2;
    private final AtomicBoolean rewind = new AtomicBoolean(false);

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        OppfolgingsbrukerKafkaDTO oppfolginsbruker = fromJson(kafkaMelding, OppfolgingsbrukerKafkaDTO.class);
        OppfolginsbrukerRepositoryV2.LeggTilEllerEndreOppfolgingsbruker(oppfolginsbruker);
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



