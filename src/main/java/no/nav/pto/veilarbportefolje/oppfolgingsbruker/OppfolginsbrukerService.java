package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erPostgresPa;

@Service
@Slf4j
@RequiredArgsConstructor
public class OppfolginsbrukerService implements KafkaConsumerService<String> {
    private final OppfolginsbrukerRepositoryV2 OppfolginsbrukerRepositoryV2;
    private final UnleashService unleashService;
    private final AtomicBoolean rewind = new AtomicBoolean(false);

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        OppfolgingsbrukerKafkaDTO oppfolginsbruker = fromJson(kafkaMelding, OppfolgingsbrukerKafkaDTO.class);
        log.info("Fikk endring pa oppfolginsbruker: {}, topic: aapen-fo-endringPaaOppfoelgingsBruker-v1",oppfolginsbruker.getAktoerid());
        if(erPostgresPa(unleashService)){
            int rader = OppfolginsbrukerRepositoryV2.leggTilEllerEndreOppfolgingsbruker(oppfolginsbruker);
            log.info("Oppdatert oppfolginsbruker info for bruker: {}, i postgres rader pavirket: {}",oppfolginsbruker.getAktoerid(), rader);
        }
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



