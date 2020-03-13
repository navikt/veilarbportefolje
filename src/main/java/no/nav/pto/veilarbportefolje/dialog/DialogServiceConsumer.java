package no.nav.pto.veilarbportefolje.dialog;

import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;

public class DialogServiceConsumer implements KafkaConsumerService {

    @Override
    public void behandleKafkaMelding(String kafkaMelding, String kafkaTopic) {

    }
}
