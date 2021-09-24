package no.nav.pto.veilarbportefolje.profilering;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileringService extends KafkaCommonConsumerService<ArbeidssokerProfilertEvent> implements KafkaConsumerService<ArbeidssokerProfilertEvent> {
    private final ProfileringRepository profileringRepository;
    private final ProfileringRepositoryV2 profileringRepositoryV2;
    @Getter
    private final UnleashService unleashService;
    private final AtomicBoolean rewind = new AtomicBoolean();

    public void behandleKafkaMelding(ArbeidssokerProfilertEvent kafkaMelding) {
        behandleKafkaMeldingLogikk(kafkaMelding);
    }

    public void behandleKafkaMeldingLogikk(ArbeidssokerProfilertEvent kafkaMelding) {
        profileringRepositoryV2.upsertBrukerProfilering(kafkaMelding);
        profileringRepository.upsertBrukerProfilering(kafkaMelding);

        log.info("Oppdaterer brukerprofilering i postgres for: {}, {}, {}", kafkaMelding.getAktorid(), kafkaMelding.getProfilertTil().name(), DateUtils.zonedDateStringToTimestamp(kafkaMelding.getProfileringGjennomfort()));
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
