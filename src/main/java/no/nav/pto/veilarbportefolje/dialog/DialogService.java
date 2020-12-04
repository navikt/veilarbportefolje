package no.nav.pto.veilarbportefolje.dialog;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.common.json.JsonUtils.fromJson;

@Slf4j
@Service
public class DialogService implements KafkaConsumerService<String> {

    private final DialogRepository dialogRepository;
    private final ElasticIndexer elasticIndexer;
    private final AtomicBoolean rewind;

    @Autowired
    public DialogService(DialogRepository dialogRepository, ElasticIndexer elasticIndexer) {
        this.dialogRepository = dialogRepository;
        this.elasticIndexer = elasticIndexer;
        this.rewind = new AtomicBoolean();
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        Dialogdata melding = fromJson(kafkaMelding, Dialogdata.class);
        dialogRepository.oppdaterDialogInfoForBruker(melding);
        elasticIndexer.indekser(AktoerId.of(melding.getAktorId()));
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
