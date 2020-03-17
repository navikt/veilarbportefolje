package no.nav.pto.veilarbportefolje.registrering;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.util.KafkaUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.HashMap;

import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;


@Configuration
public class RegistreringConfig {

    static final String KAFKA_REGISTRERING_CONSUMER_TOPIC = "aapen-arbeid-arbeidssoker-registrert" + requireEnvironmentName();

    @Bean
    public Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer() {
        HashMap<String, Object> props = KafkaUtils.kafkaProperties();
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("specific.avro.reader", true);

        Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer = new KafkaConsumer<>(props);
        kafkaRegistreringConsumer.subscribe(Collections.singletonList(KAFKA_REGISTRERING_CONSUMER_TOPIC));
        return kafkaRegistreringConsumer;
    }

    @Bean
    public RegistreringRepository registreringRepository(JdbcTemplate jdbcTemplate) {
        return new RegistreringRepository(jdbcTemplate);
    }

    @Bean
    public RegistreringService registreringService(RegistreringRepository registreringRepository) {
        return new RegistreringService(registreringRepository);
    }

    @Bean
    public KafkaConsumerRegistrering kafkaConsumerRegistrering(RegistreringService registreringService, Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer) {
        return new KafkaConsumerRegistrering(registreringService, kafkaRegistreringConsumer);
    }

}
