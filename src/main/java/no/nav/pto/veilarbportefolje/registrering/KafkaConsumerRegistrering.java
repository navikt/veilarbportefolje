package no.nav.pto.veilarbportefolje.registrering;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.util.JobUtils;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.time.Duration;
import java.util.Date;

import static no.nav.pto.veilarbportefolje.util.KafkaProperties.KAFKA_BROKERS;

@Slf4j
public class KafkaConsumerRegistrering implements Helsesjekk, Runnable {
    private Consumer<String, ArbeidssokerRegistrertEvent> kafkaConsumer;
    private RegistreringService registreringService;
    private UnleashService unleashService;
    private long lastThrownExceptionTime;
    private Exception e;

    public KafkaConsumerRegistrering(RegistreringService registreringService, Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer, UnleashService unleashService) {
        this.registreringService = registreringService;
        this.unleashService = unleashService;
        this.kafkaConsumer = kafkaRegistreringConsumer;
        JobUtils.runAsyncJob(this);
    }

    @SneakyThrows
    @Override
    public void run() {

        while (this.registreringFeature()) {
            try {
                ConsumerRecords<String, ArbeidssokerRegistrertEvent> records = kafkaConsumer.poll(Duration.ofSeconds(1L));
                for (ConsumerRecord<String, ArbeidssokerRegistrertEvent> record : records) {
                    log.info("Behandler melding for på topic: " + record.topic());
                    registreringService.behandleKafkaMelding(record.value());
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

    private boolean registreringFeature() {
        return unleashService.isEnabled(FeatureToggle.KAFKA_REGISTRERING);
    }

}
