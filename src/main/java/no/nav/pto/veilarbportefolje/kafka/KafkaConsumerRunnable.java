package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.IdUtils;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.util.Optional;

import static java.lang.Thread.currentThread;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
public class KafkaConsumerRunnable implements Runnable {

    private final KafkaConsumerService kafkaService;
    private final UnleashService unleashService;
    private final KafkaConfig.Topic topic;
    private final Optional<String> featureNavn;
    private final KafkaConsumer<String, String> consumer;

    public KafkaConsumerRunnable(KafkaConsumerService kafkaService,
                                 UnleashService unleashService,
                                 KafkaConfig.Topic topic,
                                 Optional<String> featureNavn) {

        this.kafkaService = kafkaService;
        this.unleashService = unleashService;
        this.topic = topic;
        this.featureNavn = featureNavn;
        this.consumer = new KafkaConsumer<>(KafkaProperties.kafkaProperties());

        Thread consumerThread = new Thread(this);
        consumerThread.setDaemon(true);
        consumerThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> consumerThread.interrupt()));
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(singletonList(topic.topic));

            while (featureErPa() && !currentThread().isInterrupted()) {
                ConsumerRecords<String, String> records = consumer.poll(ofSeconds(1));
                records.forEach(record -> process(record));
            }

        } catch (Exception e) {
            log.error("Konsument feilet under poll() eller subscribe() for topic {} : {}", topic.name(), e);
        } finally {
            consumer.close();
            log.info("Lukket konsument");
        }
    }

    private void process(ConsumerRecord<String, String> record) {
        String correlationId = getCorrelationIdFromHeaders(record.headers());
        MDC.put(PREFERRED_NAV_CALL_ID_HEADER_NAME, correlationId);

        log.info(
                "Behandler kafka-melding med key {} og offset {} på topic {}",
                record.key(),
                record.offset(),
                record.topic()
        );

        try {
            kafkaService.behandleKafkaMelding(record.value());
        } catch (Exception e) {
            log.error(
                    "Behandling av kafka-melding feilet for key {} og offset {} på topic {}: {}",
                    record.key(),
                    record.offset(),
                    record.topic(),
                    e
            );
        }
        MDC.remove(PREFERRED_NAV_CALL_ID_HEADER_NAME);
    }

    private boolean featureErPa() {
        return this.featureNavn
                .map(featureNavn -> unleashService.isEnabled(featureNavn))
                .orElse(false);
    }

    static String getCorrelationIdFromHeaders(Headers headers) {
        return Optional.ofNullable(headers.lastHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME))
                .map(Header::value)
                .map(String::new)
                .orElse(IdUtils.generateId());
    }

}
