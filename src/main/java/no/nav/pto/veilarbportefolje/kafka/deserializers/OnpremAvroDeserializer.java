package no.nav.pto.veilarbportefolje.kafka.deserializers;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import no.nav.common.utils.EnvironmentUtils;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class OnpremAvroDeserializer<T> {
    private static final String KAFKA_SCHEMAS_URL = EnvironmentUtils.getRequiredProperty("KAFKA_SCHEMAS_URL");

    public Deserializer<T> getDeserializer() {
        return Deserializers.onPremAvroDeserializer(KAFKA_SCHEMAS_URL,
                Map.of(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true,
                        KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, KAFKA_SCHEMAS_URL));
    }
}
