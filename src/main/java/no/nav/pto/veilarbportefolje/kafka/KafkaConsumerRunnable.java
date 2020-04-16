package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.jobutils.JobUtils;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static no.nav.pto.veilarbportefolje.util.KafkaProperties.KAFKA_BROKERS;

@Slf4j
public class KafkaConsumerRunnable implements Helsesjekk, Runnable {

    private KafkaConsumerService kafkaService;
    private UnleashService unleashService;
    private KafkaConfig.Topic topic;
    private Optional<String> featureNavn;
    private long lastThrownExceptionTime;
    private Exception e;
    private KafkaConsumer<String, String> kafkaConsumer;

    public KafkaConsumerRunnable(KafkaConsumerService kafkaService, UnleashService unleashService, KafkaConfig.Topic topic, Optional<String> featureNavn) {
        this.kafkaService = kafkaService;
        this.unleashService = unleashService;
        this.topic = topic;
        this.featureNavn = featureNavn;
        this.kafkaConsumer = new KafkaConsumer<>(KafkaProperties.kafkaProperties());
        kafkaConsumer.subscribe(Collections.singletonList(topic.name()));

        JobUtils.runAsyncJob(this);
    }

    @Override
    public void run() {
        while (featureErPa()) {
            try {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1L));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Behandler melding for aktorId: {}  på topic: " + record.topic());
                    kafkaService.behandleKafkaMelding(record.value());
                }
            } catch (Exception e) {
                this.e = e;
                this.lastThrownExceptionTime = System.currentTimeMillis();
                log.error("Feilet på {} : {}", topic.name(), e);
            } finally {
                kafkaConsumer.close();
            }
        }
    }

    @Override
    public void helsesjekk() {
        if ((this.lastThrownExceptionTime + 60_000L) > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Kafka consumer feilet " + new Date(this.lastThrownExceptionTime), this.e);
        }
    }

    private boolean featureErPa() {
        return this.featureNavn
                .map(featureNavn -> unleashService.isEnabled(featureNavn))
                .orElse(false);
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka", KAFKA_BROKERS, "kafka", false);
    }

}
