package no.nav.pto.veilarbportefolje.registrering;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.jobutils.JobUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;

import java.time.Duration;
import java.util.Date;

import static no.nav.pto.veilarbportefolje.util.KafkaUtils.KAFKA_BROKERS;

@Slf4j
public class KafkaConsumerRegistrering implements Helsesjekk, Runnable {
    private Consumer<String, ArbeidssokerRegistrertEvent> kafkaConsumer;
    private RegistreringService registreringService;
    private long lastThrownExceptionTime;
    private Exception e;

    public KafkaConsumerRegistrering(RegistreringService registreringService, Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer) {
        this.registreringService = registreringService;
        this.kafkaConsumer = kafkaRegistreringConsumer;
        JobUtils.runAsyncJob(this);
    }

    @Override
    public void run() {
        while (true) {
            try {
                ConsumerRecords<String, ArbeidssokerRegistrertEvent> records = kafkaConsumer.poll(Duration.ofSeconds(1L));
                for (ConsumerRecord<String, ArbeidssokerRegistrertEvent> record : records) {
                    log.info("Behandler melding for på topic: " + record.topic());
                    registreringService.behandleKafkaMelding(record.value());
                    kafkaConsumer.commitSync();
                }
            } catch (Exception e) {
                this.e = e;
                this.lastThrownExceptionTime = System.currentTimeMillis();
                log.error("Feilet ved behandling av kafka-registrering-melding", e);
            }
        }
    }

    @Override
    public void helsesjekk() {
        if ((this.lastThrownExceptionTime + 60_000L) > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Kafka registreringsconsumer feilet " + new Date(this.lastThrownExceptionTime), this.e);
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka", KAFKA_BROKERS, "kafka", false);
    }

}
