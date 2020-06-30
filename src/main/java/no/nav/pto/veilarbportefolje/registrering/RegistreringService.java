package no.nav.pto.veilarbportefolje.registrering;

import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;

@Slf4j
public class RegistreringService implements KafkaConsumerService<ArbeidssokerRegistrertEvent> {
    private final RegistreringRepository registreringRepository;

    public RegistreringService(RegistreringRepository registreringRepository) {
        this.registreringRepository = registreringRepository;
    }

    public void behandleKafkaMelding(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        registreringRepository.upsertBrukerRegistrering(kafkaRegistreringMelding);
    }
}
