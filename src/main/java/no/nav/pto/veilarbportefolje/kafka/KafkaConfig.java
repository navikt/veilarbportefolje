package no.nav.pto.veilarbportefolje.kafka;

import no.nav.pto.veilarbportefolje.dialog.DialogServiceConsumer;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.HashMap;

import static no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService.KAFKA_VEDTAK_CONSUMER_TOPIC;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

@Configuration
public class KafkaConfig {

    public static final String KAFKA_BROKERS_URL_PROPERTY = "KAFKA_BROKERS_URL";
    protected static final String KAFKA_BROKERS = getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY);
    private static final String USERNAME = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);

    protected static final String KAFKA_DIALOG_CONSUMER_TOPIC = "aapen-oppfolging-endringPaaDialog-v1-" + requireEnvironmentName();


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

    @Bean
    public KafkaConsumer<String, String> kafkaConsumer() {
        return new KafkaConsumer<>(kafkaProperties());
    }

    @Bean
    public KafkaConsumerServiceRunnable kafkaVedtakConsumerRunnable(VedtakService vedtakService, UnleashService unleashService, KafkaConsumer kafkaConsumer) {
        return new KafkaConsumerServiceRunnable(vedtakService, unleashService, kafkaConsumer, KAFKA_VEDTAK_CONSUMER_TOPIC, "veilarbportfolje-hent-data-fra-vedtakstotte");
    }

    @Bean
    public KafkaConsumerServiceRunnable kafkaDialogConsumerRunnable(DialogServiceConsumer dialogService, UnleashService unleashService, KafkaConsumer kafkaConsumer) {
        return new KafkaConsumerServiceRunnable(dialogService, unleashService, kafkaConsumer, KAFKA_DIALOG_CONSUMER_TOPIC, "veilarbdialog.kafka");
    }

}
