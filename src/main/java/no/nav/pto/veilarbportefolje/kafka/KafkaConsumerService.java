package no.nav.pto.veilarbportefolje.kafka;

import no.nav.pto.veilarbportefolje.util.Result;

public interface KafkaConsumerService<T> {

    Result<T> behandleKafkaMelding(T kafkaMelding);
}
