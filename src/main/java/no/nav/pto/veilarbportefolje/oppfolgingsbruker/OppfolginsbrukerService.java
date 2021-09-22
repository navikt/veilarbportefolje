package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;

@Service
@Slf4j
@RequiredArgsConstructor
public class OppfolginsbrukerService extends KafkaCommonConsumerService<OppfolgingsbrukerKafkaDTO> implements KafkaConsumerService<String> {
    private final OppfolginsbrukerRepositoryV2 OppfolginsbrukerRepositoryV2;
    @Getter
    private final UnleashService unleashService;
    private final AtomicBoolean rewind = new AtomicBoolean(false);

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        OppfolgingsbrukerKafkaDTO oppfolginsbruker = fromJson(kafkaMelding, OppfolgingsbrukerKafkaDTO.class);
        behandleKafkaMeldingLogikk(oppfolginsbruker);
    }

    @Override
    protected void behandleKafkaMeldingLogikk(OppfolgingsbrukerKafkaDTO oppfolginsbruker) {
        log.info("Fikk endring pa oppfolginsbruker: {}, topic: aapen-fo-endringPaaOppfoelgingsBruker-v1", oppfolginsbruker.getAktoerid());
        int rader = OppfolginsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(oppfolginsbruker);
        log.info("Oppdatert oppfolginsbruker info for bruker: {}, i postgres rader pavirket: {}", oppfolginsbruker.getAktoerid(), rader);
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



