package no.nav.pto.veilarbportefolje.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.common.kafka.consumer.util.deserializer.AvroDeserializer;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.common.utils.EnvironmentUtils;

import java.util.Map;

public class KafkaAivenAvroDeserializer<T> {
    private static final String KAFKA_SCHEMAS_URL = EnvironmentUtils.getRequiredProperty("KAFKA_SCHEMAS_URL");

    public AvroDeserializer<T> getAivenAvroDeserializer() {
        AvroDeserializer<T> avroDeserializer = (AvroDeserializer<T>) Deserializers.aivenAvroDeserializer();
        avroDeserializer.configure(Map.of(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, KAFKA_SCHEMAS_URL,
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true), false);
        return avroDeserializer;
    }
}
