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
import static no.nav.pto.veilarbportefolje.kafka.KafkaConfig.KAFKA_BROKERS;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

@Slf4j
public class KafkaVedtakstotteServiceRunnable implements Helsesjekk, Runnable {

    private VedtakService vedtakService;
    private UnleashService unleashService;
    private KafkaConsumer<String, String> kafkaConsumer;
    private long lastThrownExceptionTime;
    private Exception e;

    public KafkaVedtakstotteServiceRunnable(VedtakService vedtakService, UnleashService unleashService, KafkaConsumer<String, String> kafkaConsumer) {
        this.vedtakService  = vedtakService;
        this.unleashService = unleashService;
        this.kafkaConsumer = kafkaConsumer;;
        JobUtils.runAsyncJob(this::run);
    }

    @Override
    public void run() {
        while (this.vedstakstotteFeatureErPa()) {
            try {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1L));
                for (ConsumerRecord<String, String> record : records) {
                    KafkaVedtakStatusEndring melding = fromJson(record.value(), KafkaVedtakStatusEndring.class);
                    log.info("Behandler melding for aktorId: {}  på topic: " + record.topic());
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
            throw new IllegalArgumentException("Kafka veilarbportefolje-vedtakstotte-consumer feilet " + new Date(this.lastThrownExceptionTime), this.e);
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
