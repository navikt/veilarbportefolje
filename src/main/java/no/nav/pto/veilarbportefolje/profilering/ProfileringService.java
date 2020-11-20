package no.nav.pto.veilarbportefolje.profilering;

import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;


@Service
public class ProfileringService implements KafkaConsumerService<ArbeidssokerProfilertEvent> {
    private final ProfileringRepository profileringRepository;
    private final AtomicBoolean rewind;

    @Autowired
    public ProfileringService(ProfileringRepository profileringRepository) {
        this.profileringRepository = profileringRepository;
        this.rewind = new AtomicBoolean();
    }

    public void behandleKafkaMelding (ArbeidssokerProfilertEvent kafkaMelding) {
        profileringRepository.upsertBrukerProfilering(kafkaMelding);
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
