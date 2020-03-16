package no.nav.pto.veilarbportefolje.kafka;

import lombok.val;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.jobutils.JobUtils;
import no.nav.pto.veilarbportefolje.registrering.KafkaRegistreringMelding;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.vedtakstotte.KafkaVedtakStatusEndring;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

public class KafkaRegistreringRunnable  implements Helsesjekk, Runnable {
    protected static final String KAFKA_REGISTRERING_CONSUMER_TOPIC = "aapen-arbeid-arbeidssoker-registrert" + requireEnvironmentName();
    private KafkaConsumer<String, String> kafkaConsumer;
    private RegistreringService registreringService;

    public KafkaRegistreringRunnable(RegistreringService registreringService) {
        HashMap<String, Object> props = KafkaUtils.kafkaProperties();
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class); // SKRIVER ØVER DEFAULT APACHE.KAFKA.COMMON.StringDeserializer
        //props.put(VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvro.class); // SKRIVER ØVER DEFAULT ACHE.KAFKA.COMMON.StringSerializer

        this.registreringService = registreringService;
        this.kafkaConsumer = new KafkaConsumer<>(props);
        this.kafkaConsumer.subscribe(Collections.singletonList(KAFKA_REGISTRERING_CONSUMER_TOPIC));
        JobUtils.runAsyncJob(this::run);
    }


    @Override
    public void run() {
        /*
        while (true) {
            try {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1L));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Behandler melding for på topic:" + record.topic());
                    KafkaVedtakStatusEndring melding = fromJson(record.value(), KafkaVedtakStatusEndring.class);
                    registreringService.behandleKafkaMelding(melding);
                    kafkaConsumer.commitSync();
                }
            } catch (Exception e) {
                this.e = e;
                this.lastThrownExceptionTime = System.currentTimeMillis();
                log.error("Feilet ved behandling av kafka-vedtaksstotte-melding", e);
            }
        }
         */
    }


    @Override
    public void helsesjekk() throws Throwable {

    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return null;
    }
}
