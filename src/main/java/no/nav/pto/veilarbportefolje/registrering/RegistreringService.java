package no.nav.pto.veilarbportefolje.registrering;

import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;

public class RegistreringService {
    private RegistreringRepository registreringRepository;

    public RegistreringService(RegistreringRepository registreringRepository) {
        this.registreringRepository = registreringRepository;
    }

    public void behandleKafkaMelding(ArbeidssokerRegistrertEvent kafkaRegistreringMelding) {
        registreringRepository.insertBrukerRegistrering(kafkaRegistreringMelding);
    }

}
