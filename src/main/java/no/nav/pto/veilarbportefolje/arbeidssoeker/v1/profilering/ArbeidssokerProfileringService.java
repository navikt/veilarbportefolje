package no.nav.pto.veilarbportefolje.arbeidssoeker.v1.profilering;

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
public class ArbeidssokerProfileringService extends KafkaCommonConsumerService<ArbeidssokerProfilertEvent> {
    private final ArbeidssokerProfileringRepositoryV2 arbeidssokerProfileringRepositoryV2;

    public void behandleKafkaMeldingLogikk(ArbeidssokerProfilertEvent kafkaMelding) {
        arbeidssokerProfileringRepositoryV2.upsertBrukerProfilering(kafkaMelding);
        secureLog.info("Oppdaterer brukerprofilering i postgres for: {}, {}, {}", kafkaMelding.getAktorid(), kafkaMelding.getProfilertTil().name(), DateUtils.zonedDateStringToTimestamp(kafkaMelding.getProfileringGjennomfort()));
    }
}
