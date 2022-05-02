package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OppfolgingsbrukerWrapperService  extends KafkaCommonConsumerService<EndringPaaOppfoelgingsBrukerV2> {
    private final OppfolgingsbrukerService oppfolgingsbrukerService;
    private final OppfolgingsbrukerServiceV2 oppfolgingsbrukerServiceV2;

    @Override
    public void behandleKafkaMeldingLogikk(EndringPaaOppfoelgingsBrukerV2 kafkaMelding) {
        oppfolgingsbrukerService.behandleKafkaMeldingLogikk(kafkaMelding);
        oppfolgingsbrukerServiceV2.behandleKafkaMeldingLogikk(kafkaMelding);
    }
}
