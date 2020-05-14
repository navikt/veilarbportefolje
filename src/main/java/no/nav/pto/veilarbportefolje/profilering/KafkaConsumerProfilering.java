package no.nav.pto.veilarbportefolje.profilering;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.util.JobUtils;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.time.Duration;

@Slf4j
public class KafkaConsumerProfilering implements Runnable {
    private Consumer<String, ArbeidssokerRegistrertEvent> kafkaConsumer;
    private RegistreringService registreringService;
    private UnleashService unleashService;

    public KafkaConsumerProfilering(RegistreringService registreringService, Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer, UnleashService unleashService) {
        this.registreringService = registreringService;
        this.unleashService = unleashService;
        this.kafkaConsumer = kafkaRegistreringConsumer;
        JobUtils.runAsyncJob(this);
    }

    @SneakyThrows
    @Override
    public void run() {

        while (this.profileringFeature()) {
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


    private boolean profileringFeature() {
        return unleashService.isEnabled("veilarbportfolje.registrering");
    }
}
