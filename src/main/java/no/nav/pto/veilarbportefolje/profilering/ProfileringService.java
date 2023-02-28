package no.nav.pto.veilarbportefolje.profilering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.springframework.stereotype.Service;

import static no.nav.pto.veilarbportefolje.util.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileringService extends KafkaCommonConsumerService<ArbeidssokerProfilertEvent> {
    private final ProfileringRepositoryV2 profileringRepositoryV2;

    public void behandleKafkaMeldingLogikk(ArbeidssokerProfilertEvent kafkaMelding) {
        profileringRepositoryV2.upsertBrukerProfilering(kafkaMelding);
        secureLog.info("Oppdaterer brukerprofilering i postgres for: {}, {}, {}", kafkaMelding.getAktorid(), kafkaMelding.getProfilertTil().name(), DateUtils.zonedDateStringToTimestamp(kafkaMelding.getProfileringGjennomfort()));
    }
}
