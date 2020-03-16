package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.jobutils.JobUtils;
import no.nav.pto.veilarbportefolje.registrering.KafkaRegistreringMelding;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
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
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

@Slf4j
public class KafkaConsumerRunnable implements Helsesjekk, Runnable {

    private VedtakService vedtakService;
    private UnleashService unleashService;
    private RegistreringService registreringService;


    private long lastThrownExceptionTime;
    private Exception e;
    private KafkaConsumer<String, String> kafkaConsumer;

    public static final String KAFKA_BROKERS_URL_PROPERTY = "KAFKA_BROKERS_URL";
    protected static final String KAFKA_BROKERS = getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY);
    private static final String USERNAME = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);
    protected static final String KAFKA_VEDTAKSTOTTE_CONSUMER_TOPIC = "aapen-oppfolging-vedtakStatusEndring-v1-" + requireEnvironmentName();
    protected static final String KAFKA_REGISTRERING_CONSUMER_TOPIC = "aapen-arbeid-arbeidssoker-registrert" + requireEnvironmentName();



    public KafkaConsumerRunnable (VedtakService vedtakService, UnleashService unleashService, RegistreringService registreringService) {
        // TODO SKA DENNA TA IN TOPICS ELLER SKA VI DEFINIERA ALLA TOPICS HER?
        // TODO SWITCH CASE PÅ TOPIC record.topic() ELLER LAGA EN NY INSTANSE AV DENNA KLASS FØR VARJE TOPIC ?
        this.kafkaConsumer = new KafkaConsumer<>(kafkaProperties());
        this.kafkaConsumer.subscribe(Arrays.asList(KAFKA_VEDTAKSTOTTE_CONSUMER_TOPIC, KAFKA_REGISTRERING_CONSUMER_TOPIC));

        this.vedtakService = vedtakService;
        this.registreringService = registreringService;
        this.unleashService = unleashService;

        JobUtils.runAsyncJob(this::run);
    }

    @Override
    public void run() {
        while (true) {
            try {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(1L));
                for (ConsumerRecord<String, String> record : records) {
                    String topic = record.topic();
                    log.info("Behandler melding for på topic:" + topic);
                    if(topic.equals(KAFKA_VEDTAKSTOTTE_CONSUMER_TOPIC) && this.vedstakstotteFeatureErPa()){
                        KafkaVedtakStatusEndring melding = fromJson(record.value(), KafkaVedtakStatusEndring.class);
                        vedtakService.behandleMelding(melding);
                        kafkaConsumer.commitSync();

                    } else if(topic.equals(KAFKA_REGISTRERING_CONSUMER_TOPIC)) {
                        KafkaRegistreringMelding melding = fromJson(record.value(), KafkaRegistreringMelding.class);
                        registreringService.behandleKafkaMelding(melding);
                        kafkaConsumer.commitSync();
                    }

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

    public static HashMap<String, Object> kafkaProperties () {
        HashMap<String, Object>  props = new HashMap<> ();
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
