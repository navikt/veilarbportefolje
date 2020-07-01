package no.nav.pto.veilarbportefolje.kafka;

public interface KafkaConsumerService<T> {
    void behandleKafkaMelding(T kafkaMelding);
    boolean shouldRewind();
    void setRewind(boolean rewind);
}
