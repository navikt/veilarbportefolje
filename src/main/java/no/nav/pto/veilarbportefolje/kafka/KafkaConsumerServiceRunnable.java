package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.jobutils.JobUtils;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;

import static no.nav.pto.veilarbportefolje.kafka.KafkaConfig.KAFKA_BROKERS;


@Slf4j
public class KafkaConsumerServiceRunnable implements Helsesjekk, Runnable {

    private KafkaConsumerService kafkaConsumerService;
    private UnleashService unleashService;
    private Consumer<String, String> kafkaConsumer;
    private String toggleName;
    private String topicName;

    private long lastThrownExceptionTime;

    private Exception e;

    public KafkaConsumerServiceRunnable(KafkaConsumerService kafkaConsumerService, UnleashService unleashService, Consumer<String, String> kafkaConsumer, String topicName, String toggleName) {
        this.kafkaConsumerService  = kafkaConsumerService;
        this.unleashService = unleashService;
        this.kafkaConsumer = kafkaConsumer;
        this.toggleName = toggleName;
        this.topicName = topicName;

        kafkaConsumer.subscribe(Arrays.asList(topicName));

        JobUtils.runAsyncJob(this::run);
    }

    @Override
    public void run() {
        while (this.featureErPa()) {
            try {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1L));
                for (ConsumerRecord<String, String> record : records) {
                    kafkaConsumerService.behandleKafkaMelding(record.value(), record.topic());
                    kafkaConsumer.commitSync();
                }
            }
            catch (Exception e) {
                this.e = e;
                this.lastThrownExceptionTime = System.currentTimeMillis();
                log.error(String.format("Feilet ved behandling av melding på topicen %s , %s", this.topicName, e));
            }
        }
    }

    @Override
    public void helsesjekk() {
        if ((this.lastThrownExceptionTime + 60_000L) > System.currentTimeMillis()) {
            throw new IllegalArgumentException(String.format("Feilet å konsumera på topicen  %s,  %s" , this.topicName,  new Date(this.lastThrownExceptionTime)), this.e);
        }
    }

    private boolean featureErPa () {
        return unleashService.isEnabled(this.toggleName);
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka", KAFKA_BROKERS, "kafka", false);
    }

}
