package no.nav.pto.veilarbportefolje.profilering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erPostgresPa;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileringService implements KafkaConsumerService<ArbeidssokerProfilertEvent> {
    private final ProfileringRepository profileringRepository;
    private final ProfileringRepositoryV2 profileringRepositoryV2;
    private final UnleashService unleashService;
    private final AtomicBoolean rewind = new AtomicBoolean();

    public void behandleKafkaMelding(ArbeidssokerProfilertEvent kafkaMelding) {
        if (erPostgresPa(unleashService)) {
            profileringRepositoryV2.upsertBrukerProfilering(kafkaMelding);
        }
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
