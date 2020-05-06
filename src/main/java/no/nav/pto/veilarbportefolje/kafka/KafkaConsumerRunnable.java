package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.common.utils.IdUtils;
import no.nav.jobutils.JobUtils;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
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
        kafkaConsumer.subscribe(Collections.singletonList(topic.topic));

        JobUtils.runAsyncJob(this);
    }

    @Override
    public void run() {
        while (featureErPa()) {
            try {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1L));
                for (ConsumerRecord<String, String> record : records) {
                    log.info(
                            "Konsumerer kafka-melding med key {} og offset {} på topic {}",
                            record.key(),
                            record.offset(),
                            record.topic()
                    );
                    kafkaService.behandleKafkaMelding(record.value());
                }
            } catch (Exception e) {
                this.e = e;
                this.lastThrownExceptionTime = System.currentTimeMillis();
                log.error("Feilet på {} : {}", topic.name(), e);
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


    static String getCorrelationIdFromHeaders(Headers headers) {
        return Optional.ofNullable(headers.lastHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME))
                .map(Header::value)
                .map(String::new)
                .orElse(IdUtils.generateId());
    }

}
