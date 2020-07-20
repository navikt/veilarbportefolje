package no.nav.pto.veilarbportefolje.registrering;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.util.JobUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.time.Duration;

@Slf4j
public class KafkaConsumerRegistrering implements Runnable {
    private Consumer<String, ArbeidssokerRegistrertEvent> kafkaConsumer;
    private RegistreringService registreringService;
    private UnleashService unleashService;

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
                log.error("Feilet ved behandling av kafka-registrering-melding", e);
            }
        }
    }

    private boolean registreringFeature() {
        return unleashService.isEnabled(FeatureToggle.KAFKA_REGISTRERING);
    }

}
