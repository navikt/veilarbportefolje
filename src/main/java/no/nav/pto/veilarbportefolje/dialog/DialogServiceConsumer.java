package no.nav.pto.veilarbportefolje.dialog;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.feed.dialog.DialogDataFraFeed;
import no.nav.pto.veilarbportefolje.feed.dialog.DialogFeedRepository;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;

@Slf4j
public class DialogServiceConsumer implements KafkaConsumerService {

    private DialogFeedRepository dialogFeedRepository;
    private ElasticIndexer elasticIndexer;

    public static final String DIALOG_KAFKA_TOGGLE = "veilarbdialog.kafka";

    public static final String KAFKA_DIALOG_CONSUMER_TOPIC = "aapen-oppfolging-endringPaaDialog-v1-" + requireEnvironmentName();

    public DialogServiceConsumer(DialogFeedRepository dialogFeedRepository, ElasticIndexer elasticIndexer) {
        this.dialogFeedRepository = dialogFeedRepository;
        this.elasticIndexer = elasticIndexer;
    }

    @Override
    public void behandleKafkaMelding(String kafkaMelding, String kafkaTopic) {
        if(kafkaTopic.equals(KAFKA_DIALOG_CONSUMER_TOPIC)) {
            DialogDataFraFeed melding = fromJson(kafkaMelding, DialogDataFraFeed.class);
            log.info("Behandler melding for aktorId: {}  på topic: " + kafkaTopic);
            dialogFeedRepository.oppdaterDialogInfoForBruker(melding);
            elasticIndexer.indekserAsynkront(AktoerId.of(melding.getAktorId()));
        }
    }
}
