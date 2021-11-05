package no.nav.pto.veilarbportefolje.mal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MalService extends KafkaCommonConsumerService<MalEndringKafkaDTO> {

    private final SisteEndringService sisteEndringService;

    @Override
    protected void behandleKafkaMeldingLogikk(MalEndringKafkaDTO melding) {
        sisteEndringService.behandleMal(melding);
    }

}