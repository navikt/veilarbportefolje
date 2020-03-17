package no.nav.pto.veilarbportefolje.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.kafka.KafkaRegistreringRunnable;
import no.nav.pto.veilarbportefolje.kafka.KafkaVedtakStotteConsumerRunnable;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Collections;
import java.util.HashMap;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;


@Configuration
public class KafkaConfig {

    public static final String KAFKA_BROKERS_URL_PROPERTY = "KAFKA_BROKERS_URL";
    public static final String KAFKA_BROKERS = getRequiredProperty(KAFKA_BROKERS_URL_PROPERTY);
    private static final String USERNAME = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_USERNAME);
    private static final String PASSWORD = getRequiredProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD);
    private static String KAFKA_VEDTAKSTOTTE_CONSUMER_TOPIC = "aapen-oppfolging-vedtakStatusEndring-v1-" + requireEnvironmentName();
    public static final String KAFKA_REGISTRERING_CONSUMER_TOPIC = "aapen-arbeid-arbeidssoker-registrert" + requireEnvironmentName();

    @Bean
    public Consumer<String, String> kafkaVedtakStotteConsumer() {
        Consumer<String, String> kafkaVedtakStotteConsumer = new KafkaConsumer<>(kafkaProperties());
        kafkaVedtakStotteConsumer.subscribe(Collections.singletonList(KAFKA_VEDTAKSTOTTE_CONSUMER_TOPIC));
        return  kafkaVedtakStotteConsumer;
    }

    @Bean
    public Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer() {
        HashMap<String, Object> props = kafkaProperties();
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("specific.avro.reader", true);

        Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer = new KafkaConsumer<>(props);
        kafkaRegistreringConsumer.subscribe(Collections.singletonList(KAFKA_REGISTRERING_CONSUMER_TOPIC));
        return kafkaRegistreringConsumer;
    }

    @Bean
    public KafkaVedtakStotteConsumerRunnable kafkaConsumerRunnable(VedtakService vedtakService, UnleashService unleashService, Consumer<String, String> kafkaVedtakStotteConsumer) {
        return new KafkaVedtakStotteConsumerRunnable(vedtakService, unleashService, kafkaVedtakStotteConsumer);
    }

    @Bean
    public KafkaRegistreringRunnable kafkaRegistreringRunnable(RegistreringService registreringService, Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer) {
        return new KafkaRegistreringRunnable(registreringService, kafkaRegistreringConsumer);
    }


    static HashMap<String, Object> kafkaProperties () {
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
