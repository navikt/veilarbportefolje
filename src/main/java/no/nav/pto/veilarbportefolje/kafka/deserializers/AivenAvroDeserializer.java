package no.nav.pto.veilarbportefolje.kafka.deserializers;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class AivenAvroDeserializer<T> {
    public Deserializer<T> getDeserializer() {
        Deserializer<T> avroDeserializer = Deserializers.aivenAvroDeserializer();
        avroDeserializer.configure(Map.of(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true), false);
        return avroDeserializer;
    }
}
