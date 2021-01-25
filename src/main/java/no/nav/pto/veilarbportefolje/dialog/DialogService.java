package no.nav.pto.veilarbportefolje.dialog;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.elastic.ElasticServiceV2;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;

@Slf4j
@Service
public class DialogService implements KafkaConsumerService<String> {

    private final DialogRepository dialogRepository;
    private final ElasticServiceV2 elasticServiceV2;
    private final AtomicBoolean rewind;

    @Autowired
    public DialogService(DialogRepository dialogRepository, ElasticServiceV2 elasticServiceV2) {
        this.dialogRepository = dialogRepository;
        this.elasticServiceV2 = elasticServiceV2;
        this.rewind = new AtomicBoolean();
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        Dialogdata melding = fromJson(kafkaMelding, Dialogdata.class);
        dialogRepository.oppdaterDialogInfoForBruker(melding);
        elasticServiceV2.updateDialog(melding);
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
