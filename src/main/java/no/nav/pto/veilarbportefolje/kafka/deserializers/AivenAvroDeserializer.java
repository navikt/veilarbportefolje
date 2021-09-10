package no.nav.pto.veilarbportefolje.kafka.deserializers;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import no.nav.common.kafka.consumer.util.deserializer.AvroDeserializer;
import no.nav.common.kafka.consumer.util.deserializer.Deserializers;

import java.util.Map;

public class AivenAvroDeserializer<T> {

    public AvroDeserializer<T> getDeserializer() {
        AvroDeserializer<T> avroDeserializer = (AvroDeserializer<T>) Deserializers.aivenAvroDeserializer();
        avroDeserializer.configure(Map.of(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true), false);
        return avroDeserializer;
    }
}
