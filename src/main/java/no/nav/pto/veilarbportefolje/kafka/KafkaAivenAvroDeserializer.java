package no.nav.pto.veilarbportefolje.kafka;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class KafkaAivenAvroDeserializer<T> extends KafkaAvroDeserializer {

    public KafkaAivenAvroDeserializer() {
        super();
        Deserializer<T> objectDeserializer = Deserializers.aivenAvroDeserializer();
        objectDeserializer.configure(Map.of(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true), false);
    }
}
