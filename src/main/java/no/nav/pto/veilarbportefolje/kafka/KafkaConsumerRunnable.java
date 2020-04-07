package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.jobutils.JobUtils;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.time.Duration;
import java.util.Date;

import static java.util.Arrays.asList;
import static no.nav.pto.veilarbportefolje.util.KafkaProperties.KAFKA_BROKERS;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;

@Slf4j
public class KafkaConsumerRunnable implements Helsesjekk, Runnable {

    public enum Topic {
        VEDTAK_STATUS_ENDRING("aapen-oppfolging-vedtakStatusEndring-v1-" + requireEnvironmentName()),
        DIALOG_CONSUMER_TOPIC("aapen-oppfolging-endringPaaDialog-v1-" + requireEnvironmentName());

        private final String topic;

        Topic(String topic) {
            this.topic = topic;
        }
    }

    private VedtakService vedtakService;
    private DialogService dialogService;
    private UnleashService unleashService;


    private long lastThrownExceptionTime;
    private Exception e;
    private Consumer<String, String> kafkaConsumer;

    public KafkaConsumerRunnable(DialogService dialogService, VedtakService vedtakService, UnleashService unleashService, Consumer<String, String> kafkaConsumer) {
        this.unleashService = unleashService;

        this.kafkaConsumer = kafkaConsumer;
        this.kafkaConsumer.subscribe(asList(Topic.VEDTAK_STATUS_ENDRING.topic, Topic.DIALOG_CONSUMER_TOPIC.topic));
        this.vedtakService = vedtakService;
        this.dialogService = dialogService;

        JobUtils.runAsyncJob(this::run);
    }

    @Override
    public void run() {

        while (true) {
            try {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1L));
                for (ConsumerRecord<String, String> record : records) {
                    routeMessage(record);
                }

            } catch (Exception e) {
                this.e = e;
                this.lastThrownExceptionTime = System.currentTimeMillis();
                log.error("Feilet ved behandling av kafka-melding", e);
            } finally {
                kafkaConsumer.close();
            }
        }
    }

    private void routeMessage(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        String melding = record.value();
        log.info("Behandler melding for aktorId: {}  på topic: " + topic);
        switch (Topic.valueOf(topic)) {
            case VEDTAK_STATUS_ENDRING:
                if (this.vedstakstotteFeatureErPa()) {
                    vedtakService.behandleKafkaMelding(melding);
                }
                break;
            case DIALOG_CONSUMER_TOPIC:
                if (this.dialogFeatureErPa()) {
                    dialogService.behandleKafkaMelding(melding);
                }
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void helsesjekk() {
        if ((this.lastThrownExceptionTime + 60_000L) > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Kafka consumer feilet " + new Date(this.lastThrownExceptionTime), this.e);
        }
    }
    private boolean vedstakstotteFeatureErPa() {
        return unleashService.isEnabled("veilarbportfolje-hent-data-fra-vedtakstotte");
    }

    private boolean dialogFeatureErPa() {
        return unleashService.isEnabled("veilarbdialog.kafka");
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka", KAFKA_BROKERS, "kafka", false);
    }

}
