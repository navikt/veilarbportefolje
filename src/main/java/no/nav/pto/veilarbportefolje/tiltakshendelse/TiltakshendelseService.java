package no.nav.pto.veilarbportefolje.tiltakshendelse;

import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.tiltakshendelse.dto.input.Tiltakshendelse;
import org.springframework.stereotype.Service;

@Service
public class TiltakshendelseService extends KafkaCommonConsumerService<Tiltakshendelse> {

    @Override
    protected void behandleKafkaMeldingLogikk(Tiltakshendelse kafkaMelding) {

    }
}
