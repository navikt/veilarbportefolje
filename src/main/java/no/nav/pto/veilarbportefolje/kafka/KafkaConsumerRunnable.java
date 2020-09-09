package no.nav.pto.veilarbportefolje.kafka;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.Event;
import no.nav.common.metrics.MetricsClient;
import no.nav.common.utils.IdUtils;
import no.nav.pto.veilarbportefolje.kafka.KafkaConfig.Topic;
import no.nav.pto.veilarbportefolje.util.JobUtils;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;

@Slf4j
public class KafkaConsumerRunnable<T> implements Runnable {

    private final KafkaConsumerService<T> kafkaService;
    private final UnleashService unleashService;
    private final String topic;
    private final KafkaConsumer<String, T> consumer;
    private final AtomicBoolean shutdown;
    private final CountDownLatch shutdownLatch;
    private final MetricsClient metricsClient;

    public KafkaConsumerRunnable(KafkaConsumerService<T> kafkaService,
                                 UnleashService unleashService,
                                 Properties kafkaProperties,
                                 Topic topic,
                                 MetricsClient metricsClient) {

        this.kafkaService = kafkaService;
        this.unleashService = unleashService;
        this.topic = topic.topic;
        this.consumer = new KafkaConsumer<>(kafkaProperties);
        this.metricsClient = metricsClient;
        this.shutdown = new AtomicBoolean(false);
        this.shutdownLatch = new CountDownLatch(1);

        JobUtils.runAsyncJob(this);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }


    @Override
    public void run() {
        try {
            log.info("Starter konsument for {}", topic);
            consumer.subscribe(singletonList(topic), getRebalanceListener());
            while (!shutdown.get()) {
                ConsumerRecords<String, T> records = consumer.poll(ofSeconds(1));
                records.forEach(this::process);
                if (kafkaService.shouldRewind()) {
                    this.rewind();
                }
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

    private void rewind() {
        String topicName = this.topic;

        List<TopicPartition> partitions =
                consumer.partitionsFor(topicName)
                        .stream()
                        .map(partition -> new TopicPartition(topicName, partition.partition()))
                        .collect(toList());

        log.info("Spoler tilbake topic {}", topicName);
        consumer.seekToBeginning(partitions);
        kafkaService.setRewind(false);
    }

    private void process(ConsumerRecord<String, T> record) {
        String correlationId = getCorrelationIdFromHeaders(record.headers());
        MDC.put(PREFERRED_NAV_CALL_ID_HEADER_NAME, correlationId);

        log.info(
                "Behandler kafka-melding med key {} og offset {} på topic {}",
                record.key(),
                record.offset(),
                record.topic()
        );

        metricsClient.report(new Event(topic + ".message_consumed"));

        try {
            kafkaService.behandleKafkaMelding(record.value());
        } catch (Exception e) {
            String mld = String.format(
                    "%s for key %s, og offset %s på topic %s",
                    e.getClass().getSimpleName(),
                    record.key(),
                    record.offset(),
                    record.topic()
            );
            log.error(mld, e);
        }
        MDC.remove(PREFERRED_NAV_CALL_ID_HEADER_NAME);
    }

    private ConsumerRebalanceListener getRebalanceListener() {
        return new ConsumerRebalanceListener() {

            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {

            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                if (unleashService.isEnabled(topic + "_rewind")) {
                    log.info("Spoler tilbake til begynnelsen for topic " + topic);
                    consumer.seekToBeginning(partitions);
                }
            }
        };
    }

    static String getCorrelationIdFromHeaders(Headers headers) {
        return Optional.ofNullable(headers.lastHeader(PREFERRED_NAV_CALL_ID_HEADER_NAME))
                .map(Header::value)
                .map(String::new)
                .orElse(IdUtils.generateId());
    }

}
