package no.nav.pto.veilarbportefolje.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.common.kafka.consumer.util.deserializer.AvroDeserializer;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.common.utils.EnvironmentUtils;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class KafkaAivenAvroDeserializer extends Deserializers {
    public static <T> Deserializer<T> deserializer() {
        String schemaRegistryUrl = EnvironmentUtils.getRequiredProperty("KAFKA_SCHEMA_REGISTRY", new String[0]);
        String username = EnvironmentUtils.getRequiredProperty("KAFKA_SCHEMA_REGISTRY_USER", new String[0]);
        String password = EnvironmentUtils.getRequiredProperty("KAFKA_SCHEMA_REGISTRY_PASSWORD", new String[0]);
        AvroDeserializer avroDeserializer = new AvroDeserializer(schemaRegistryUrl, username, password);
        avroDeserializer.configure(Map.of(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true), false);
        return avroDeserializer;
    }
}
