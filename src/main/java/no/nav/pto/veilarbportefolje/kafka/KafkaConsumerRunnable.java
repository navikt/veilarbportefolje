package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.jobutils.JobUtils;
import no.nav.pto.veilarbportefolje.feed.aktivitet.AktivitetService;
import no.nav.pto.veilarbportefolje.kafka.aktivitet.KafkaAktivitetMelding;
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
import java.util.Date;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static no.nav.json.JsonUtils.fromJson;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Slf4j
public class KafkaConsumerRunnable implements Helsesjekk, Runnable {

    private VedtakService vedtakService;
    private AktivitetService aktivitetService;
    private UnleashService unleashService;


    private long lastThrownExceptionTime;
    private Exception e;
    private KafkaConsumer<String, String> kafka;

    public static final String KAFKA_BROKERS_URL_PROPERTY = "KAFKA_BROKERS_URL";
    protected static final String KAFKA_BROKERS = getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY);
    private static final String USERNAME = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);

    private enum Topic {
        VEDTAK_STATUS_ENDRING("aapen-oppfolging-vedtakStatusEndring-v1-" + requireEnvironmentName()),
        AKTIVITET_OPPDATERT("aapen-fo-aktivitetOppdatert-v1-" + requireEnvironmentName());

        private final String topic;

        Topic(String topic) {
            this.topic = topic;
        }
    }

    public KafkaConsumerRunnable(AktivitetService aktivitetService, VedtakService vedtakService, UnleashService unleashService) {
        this.kafka = new KafkaConsumer<>(kafkaProperties());
        this.kafka.subscribe(asList(Topic.VEDTAK_STATUS_ENDRING.topic, Topic.AKTIVITET_OPPDATERT.topic));
        this.vedtakService = vedtakService;
        this.unleashService = unleashService;
        this.aktivitetService = aktivitetService;

        JobUtils.runAsyncJob(this::run);
    }

    @Override
    public void run() {

        while (true) {
            try {

                ConsumerRecords<String, String> records = kafka.poll(Duration.ofSeconds(1L));

                for (ConsumerRecord<String, String> record : records) {
                    routeMessage(record);
                    kafka.commitSync();
                }

            } catch (Exception e) {
                this.e = e;
                this.lastThrownExceptionTime = System.currentTimeMillis();
                log.error("Feilet ved behandling av kafka-melding", e);
            } finally {
                kafka.close();
            }
        }
    }

    private void routeMessage(ConsumerRecord<String, String> record) {
        switch (Topic.valueOf(record.topic())) {
            case VEDTAK_STATUS_ENDRING:
                if (this.vedstakstotteFeatureErPa()) {
                    handleVedtakMelding(record);
                }
                break;
            case AKTIVITET_OPPDATERT:
                if (this.aktiviteterFraKafkaErPa()) {
                    handleAktivitetOppdatert(record);
                }
                break;
            default:
                throw new IllegalStateException();
        }
    }

    private void handleAktivitetOppdatert(ConsumerRecord<String, String> record) {
        KafkaAktivitetMelding melding = fromJson(record.value(), KafkaAktivitetMelding.class);
        log.info("Behandler melding {} fra kafka om aktivitet {} og aktorId {} på topic {} ", melding.getMeldingId(), melding.getAktivitetId(), melding.getAktorId(), record.topic());
        aktivitetService.behandleMelding(melding);
    }

    private void handleVedtakMelding(ConsumerRecord<String, String> record) {
        KafkaVedtakStatusEndring melding = fromJson(record.value(), KafkaVedtakStatusEndring.class);
        log.info("Behandler melding for aktorId: {}  på topic: " + record.topic());
        vedtakService.behandleMelding(melding);
    }

    @Override
    public void helsesjekk() {
        if ((this.lastThrownExceptionTime + 60_000L) > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Kafka consumer feilet " + new Date(this.lastThrownExceptionTime), this.e);
        }
    }

    private boolean aktiviteterFraKafkaErPa() {
        return unleashService.isEnabled("portefolje.kafka.aktivitet");
    }

    private boolean vedstakstotteFeatureErPa () {
        return unleashService.isEnabled("veilarbportfolje-hent-data-fra-vedtakstotte");
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka", KAFKA_BROKERS, "kafka", false);
    }

    public static HashMap<String, Object> kafkaProperties() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BROKERS);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        props.put(GROUP_ID_CONFIG, "veilarbportefolje-consumer");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(MAX_POLL_INTERVAL_MS_CONFIG, 5000);
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }
}
