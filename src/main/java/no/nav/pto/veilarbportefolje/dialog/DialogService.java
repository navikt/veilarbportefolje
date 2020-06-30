package no.nav.pto.veilarbportefolje.dialog;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;

import static no.nav.common.json.JsonUtils.fromJson;

@Slf4j
public class DialogService implements KafkaConsumerService<String> {

    private DialogRepository dialogRepository;
    private ElasticIndexer elasticIndexer;


    public DialogService(DialogRepository dialogRepository, ElasticIndexer elasticIndexer) {
        this.dialogRepository = dialogRepository;
        this.elasticIndexer = elasticIndexer;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        Dialogdata melding = fromJson(kafkaMelding, Dialogdata.class);
        dialogRepository.oppdaterDialogInfoForBruker(melding);
        elasticIndexer.indekser(AktoerId.of(melding.getAktorId()));
    }
}
