package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.pto.veilarbportefolje.domene.KafkaVedtakStatusEndring;
import no.nav.pto.veilarbportefolje.service.VedtakService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.kafka.KafkaConsumerConfig.KAFKA_BROKERS;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;

@Slf4j
@Component
public class KafkaConsumerRunnable implements Helsesjekk, Runnable {

    @Inject
    private VedtakService vedtakService;

    private KafkaConsumer<String, String> kafkaConsumer;
    protected static final String KAFKA_CONSUMER_TOPIC = "aapen-oppfolging-vedtakStatusEndring-v1-" + requireEnvironmentName();

    private long lastThrownExceptionTime;
    private Exception e;


    public KafkaConsumerRunnable () {
        // TODO SKA DENNA TA IN TOPICS ELLER SKA VI DEFINIERA ALLA TOPICS HER?
        kafkaConsumer = new KafkaConsumer<>(KafkaConsumerConfig.kafkaProperties());
        kafkaConsumer.subscribe(Arrays.asList(KAFKA_CONSUMER_TOPIC));
    }

    @Override
    public void run() {
        try {
            while(true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(10));
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
                    for (ConsumerRecord<String, String> record : partitionRecords) {
                        // TODO SWITCH CASE PÅ TOPIC record.topic() ELLER LAGA EN NY INSTANSE AV DENNA KLASS FØR VARJE TOPIC ?
                        KafkaVedtakStatusEndring melding = fromJson(record.value(), KafkaVedtakStatusEndring.class);
                        log.info("Leser melding for aktorId:" + melding.getAktorId() + " på topic: " + record.topic());

                        vedtakService.behandleMelding(melding);

                        //HVIS INSETTNINGEN I DB:EN VAR VEDLYKKAD SÅ COMMITTER VI OFFSETEN
                        long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                        kafkaConsumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)));
                    }
                }
            }
        } catch (Exception e) {
            this.e = e;
            this.lastThrownExceptionTime = System.currentTimeMillis();
            log.error("Feilet ved behandling av kafka-vedtaksstotte-melding", e);
        }
    }

    @Override
    public void helsesjekk() throws Throwable {
        if ((this.lastThrownExceptionTime + 60_000L) > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Kafka consumer feilet " + new Date(this.lastThrownExceptionTime), this.e);
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka", KAFKA_BROKERS, "kafka", false);
    }
}
