package no.nav.pto.veilarbportefolje.dialog;

import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;

import static no.nav.json.JsonUtils.fromJson;

public class DialogService implements KafkaConsumerService<String> {

    private DialogFeedRepository dialogFeedRepository;
    private ElasticIndexer elasticIndexer;


    public DialogService(DialogFeedRepository dialogFeedRepository, ElasticIndexer elasticIndexer) {
        this.dialogFeedRepository = dialogFeedRepository;
        this.elasticIndexer = elasticIndexer;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding) {
        DialogData melding = fromJson(kafkaMelding, DialogData.class);
        dialogFeedRepository.oppdaterDialogInfoForBruker(melding);
        elasticIndexer.indekser(AktoerId.of(melding.getAktorId()));
    }
}
