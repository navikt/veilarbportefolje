package no.nav.pto.veilarbportefolje.kafka;

import io.micrometer.core.instrument.Counter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.IdUtils;
import no.nav.pto.veilarbportefolje.util.JobUtils;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.pto.veilarbportefolje.util.Result;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;

@Slf4j
public class KafkaConsumerRunnable<T> implements Runnable {

    private final KafkaConsumerService<T> kafkaService;
    private final String topic;
    private final KafkaConsumer<String, T> consumer;
    private final AtomicBoolean shutdown;
    private final CountDownLatch shutdownLatch;
    private final Counter counter;
    private final Counter errorCounter;

    public KafkaConsumerRunnable(KafkaConsumerService<T> kafkaService, KafkaConfig.Topic topic) {

        this.kafkaService = kafkaService;
        this.topic = topic.topic;
        this.consumer = new KafkaConsumer<>(KafkaProperties.kafkaProperties());
        this.shutdown = new AtomicBoolean(false);
        this.shutdownLatch = new CountDownLatch(1);

        JobUtils.runAsyncJob(this);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.shutdown()));

        counter = Counter.builder(topic.topic + "-records_processed").register(getMeterRegistry());
        errorCounter = Counter.builder(topic.topic + "-records_failed").register(getMeterRegistry());
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(singletonList(topic));
            while (!shutdown.get()) {
                ConsumerRecords<String, T> records = consumer.poll(ofSeconds(1));
                records.forEach(record -> {
                    Result<T> result = this.process(record);
                    if (result.isErr()) {
                        errorCounter.increment();
                        return;
                    }

                    consumer.commitAsync(); // Eller hur?
                    counter.increment();
                });
            }
        } catch (Exception e) {
            String mld = String.format(
                    "%s under poll() eller subscribe() for topic %s",
                    e.getClass().getSimpleName(),
                    topic
            );
            log.error(mld, e);
        } finally {
            consumer.close();
            shutdownLatch.countDown();
            log.info("Lukket konsument for topic {}", topic);
        }
    }

    @SneakyThrows
    public void shutdown() {
        shutdown.set(true);
        shutdownLatch.await();
    }

    private Result<T> process(ConsumerRecord<String, T> record) {
        String correlationId = getCorrelationIdFromHeaders(record.headers());
        MDC.put(PREFERRED_NAV_CALL_ID_HEADER_NAME, correlationId);

        log.info(
                "Behandler kafka-melding med key {} og offset {} på topic {}",
                record.key(),
                record.offset(),
                record.topic()
        );

        Result<T> result = kafkaService.behandleKafkaMelding(record.value());
        result.onError(t -> {
            String mld = String.format(
                    "%s for key %s, og offset %s på topic %s",
                    t.getClass().getSimpleName(),
                    record.key(),
                    record.offset(),
                    record.topic()
            );
            log.error(mld, t);
        });

        MDC.remove(PREFERRED_NAV_CALL_ID_HEADER_NAME);
        return result;
    }

    static String getCorrelationIdFromHeaders(Headers headers) {
        return Optional.ofNullable(headers.lastHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME))
                .map(Header::value)
                .map(String::new)
                .orElse(IdUtils.generateId());
    }

}
