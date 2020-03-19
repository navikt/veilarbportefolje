package no.nav.pto.veilarbportefolje.registrering;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.ws.rs.client.Client;
import java.util.Collections;
import java.util.HashMap;

import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;
import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;


@Configuration
public class RegistreringConfig {

    static final String KAFKA_REGISTRERING_CONSUMER_TOPIC = "aapen-arbeid-arbeidssoker-registrert-" + requireEnvironmentName();
    static final String KAFKA_SCHEMAS_URL = getRequiredProperty("KAFKA_SCHEMAS_URL");

    @Bean
    public Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer() {
        HashMap<String, Object> props = KafkaProperties.kafkaProperties();
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
        props.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, KAFKA_SCHEMAS_URL);

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
    public KafkaConsumerRegistrering kafkaConsumerRegistrering(RegistreringService registreringService, Consumer<String, ArbeidssokerRegistrertEvent> kafkaRegistreringConsumer, UnleashService unleashService) {
        return new KafkaConsumerRegistrering(registreringService, kafkaRegistreringConsumer, unleashService);
    }

    @Bean
    public VeilarbregistreringClient veilarbregistreringClient(Client client) {
        return new VeilarbregistreringClient(client);
    }

}
