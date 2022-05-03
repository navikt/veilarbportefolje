package no.nav.pto.veilarbportefolje.oppfolgingsbruker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OppfolgingsbrukerWrapperService extends KafkaCommonConsumerService<EndringPaaOppfoelgingsBrukerV2> {
    private final OppfolgingsbrukerService oppfolgingsbrukerService;
    private final OppfolgingsbrukerServiceV2 oppfolgingsbrukerServiceV2;

    @Override
    public void behandleKafkaMeldingLogikk(EndringPaaOppfoelgingsBrukerV2 kafkaMelding) {
        try {
            oppfolgingsbrukerServiceV2.behandleKafkaMeldingLogikk(kafkaMelding);
        } catch (Exception e) {
            log.error("Error under håndtering av oppfolgingsbruker V2");
            throw e;
        }

        try {
            oppfolgingsbrukerService.behandleKafkaMeldingLogikk(kafkaMelding);
        } catch (Exception e) {
            log.error("Error under håndtering av oppfolgingsbruker V1");
            throw e;
        }
    }
}
