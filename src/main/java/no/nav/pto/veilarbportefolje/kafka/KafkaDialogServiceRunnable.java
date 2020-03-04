package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.jobutils.JobUtils;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.feed.dialog.DialogDataFraFeed;
import no.nav.pto.veilarbportefolje.feed.dialog.DialogFeedRepository;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Date;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.kafka.KafkaConfig.KAFKA_BROKERS;


@Slf4j
public class KafkaDialogServiceRunnable implements Helsesjekk, Runnable {

    private DialogFeedRepository dialogFeedRepository;
    private ElasticIndexer elasticIndexer;
    private UnleashService unleashService;
    private Consumer<String, String> kafkaConsumer;
    private long lastThrownExceptionTime;
    private Exception e;

    public KafkaDialogServiceRunnable(DialogFeedRepository dialogFeedRepository, UnleashService unleashService, Consumer<String, String> kafkaConsumer, ElasticIndexer elasticIndexer) {
        this.dialogFeedRepository  = dialogFeedRepository;
        this.unleashService = unleashService;
        this.kafkaConsumer = kafkaConsumer;
        this.elasticIndexer = elasticIndexer;
        JobUtils.runAsyncJob(this::run);
    }

    @Override
    public void run() {
        while (this.dialogKafkaFeaturePa()) {
            try {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1L));
                for (ConsumerRecord<String, String> record : records) {
                    DialogDataFraFeed melding = fromJson(record.value(), DialogDataFraFeed.class);
                    log.info("Behandler melding for aktorId: {}  på topic: " + record.topic());
                    dialogFeedRepository.oppdaterDialogInfoForBruker(melding);
                    elasticIndexer.indekserAsynkront(AktoerId.of(melding.getAktorId()));
                    kafkaConsumer.commitSync();
                }
            }
            catch (Exception e) {
                this.e = e;
                this.lastThrownExceptionTime = System.currentTimeMillis();
                log.error("Feilet ved behandling av kafka-dialog-melding", e);
            }
        }
    }

    @Override
    public void helsesjekk() {
        if ((this.lastThrownExceptionTime + 60_000L) > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Kafka veilarbportefolje-dialog-consumer feilet " + new Date(this.lastThrownExceptionTime), this.e);
        }
    }

    private boolean dialogKafkaFeaturePa () {
        return unleashService.isEnabled("veilarbdialog.kafka");
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka", KAFKA_BROKERS, "kafka", false);
    }

}
