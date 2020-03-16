package no.nav.pto.veilarbportefolje.registrering;

public class RegistreringService {
    private RegistreringRepository registreringRepository;

    public RegistreringService(RegistreringRepository registreringRepository) {
        this.registreringRepository = registreringRepository;
    }

    public void behandleKafkaMelding(KafkaRegistreringMelding kafkaRegistreringMelding) {
        registreringRepository.insertBrukerRegistrering(kafkaRegistreringMelding);
    }

}
