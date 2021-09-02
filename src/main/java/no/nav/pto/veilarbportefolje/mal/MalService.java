package no.nav.pto.veilarbportefolje.mal;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.kafka.KafkaCommonConsumerService;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.sisteendring.SisteEndringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;

@Slf4j
@Service
public class MalService extends KafkaCommonConsumerService<MalEndringKafkaDTO> implements KafkaConsumerService<String> {

    private final SisteEndringService sisteEndringService;
    private final AtomicBoolean rewind;
    @Getter
    private final UnleashService unleashService;

    @Autowired
    public MalService(SisteEndringService sisteEndringService, UnleashService unleashService) {
        this.sisteEndringService = sisteEndringService;
        this.unleashService = unleashService;
        this.rewind = new AtomicBoolean();
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        MalEndringKafkaDTO melding = fromJson(kafkaMelding, MalEndringKafkaDTO.class);
        behandleKafkaMeldingLogikk(melding);
    }

    @Override
    protected void behandleKafkaMeldingLogikk(MalEndringKafkaDTO melding) {
        sisteEndringService.behandleMal(melding);
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