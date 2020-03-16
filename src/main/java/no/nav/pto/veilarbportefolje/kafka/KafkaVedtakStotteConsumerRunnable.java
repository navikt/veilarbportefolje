package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.jobutils.JobUtils;
import no.nav.pto.veilarbportefolje.vedtakstotte.KafkaVedtakStatusEndring;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;


import java.time.Duration;
import java.util.*;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.kafka.KafkaUtils.KAFKA_BROKERS;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

@Slf4j
public class KafkaVedtakStotteConsumerRunnable implements Helsesjekk, Runnable {

    private VedtakService vedtakService;
    private UnleashService unleashService;


    private long lastThrownExceptionTime;
    private Exception e;
    private KafkaConsumer<String, String> kafkaConsumer;

    protected static final String KAFKA_VEDTAKSTOTTE_CONSUMER_TOPIC = "aapen-oppfolging-vedtakStatusEndring-v1-" + requireEnvironmentName();



    public KafkaVedtakStotteConsumerRunnable(VedtakService vedtakService, UnleashService unleashService) {
        // TODO SKA DENNA TA IN TOPICS ELLER SKA VI DEFINIERA ALLA TOPICS HER?
        // TODO SWITCH CASE PÅ TOPIC record.topic() ELLER LAGA EN NY INSTANSE AV DENNA KLASS FØR VARJE TOPIC ?
        this.kafkaConsumer = new KafkaConsumer<>(KafkaUtils.kafkaProperties());
        this.kafkaConsumer.subscribe(Arrays.asList(KAFKA_VEDTAKSTOTTE_CONSUMER_TOPIC));

        this.vedtakService = vedtakService;
        this.unleashService = unleashService;

        JobUtils.runAsyncJob(this::run);
    }

    @Override
    public void run() {
        while (this.vedstakstotteFeatureErPa()) {
            try {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1L));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Behandler melding for på topic:" + record.topic());
                    KafkaVedtakStatusEndring melding = fromJson(record.value(), KafkaVedtakStatusEndring.class);
                    vedtakService.behandleMelding(melding);
                    kafkaConsumer.commitSync();
                }
            }
            catch (Exception e) {
                this.e = e;
                this.lastThrownExceptionTime = System.currentTimeMillis();
                log.error("Feilet ved behandling av kafka-vedtaksstotte-melding", e);
            }
        }
    }

    @Override
    public void helsesjekk() {
        if ((this.lastThrownExceptionTime + 60_000L) > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Kafka consumer feilet " + new Date(this.lastThrownExceptionTime), this.e);
        }
    }

    private boolean vedstakstotteFeatureErPa () {
        return unleashService.isEnabled("veilarbportfolje-hent-data-fra-vedtakstotte");
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka", KAFKA_BROKERS, "kafka", false);
    }
}
