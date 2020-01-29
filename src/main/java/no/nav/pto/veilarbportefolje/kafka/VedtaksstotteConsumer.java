package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.VedtakStatusRepository;
import no.nav.pto.veilarbportefolje.domene.KafkaVedtakStatusEndring;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import javax.inject.Inject;

import java.time.Duration;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.kafka.KafkaConsumerConfig.KAFKA_CONSUMER_TOPIC;

@Slf4j
public class VedtaksstotteConsumer {

    @Inject
    private VedtakStatusRepository vedtakStatusRepository;

    private KafkaConsumer<String, String> kafkaConsumer;

    public VedtaksstotteConsumer(KafkaConsumer kafkaConsumer) {
        this.kafkaConsumer = kafkaConsumer;
    }

    public void consume() {
        try {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofSeconds(10));
            for (ConsumerRecord<String, String> record : records) {
                KafkaVedtakStatusEndring melding = fromJson(record.value(), KafkaVedtakStatusEndring.class);
                log.info("Leser melding for aktorId:" + melding.getAktorId() + " på topic: " + KAFKA_CONSUMER_TOPIC );
                if(melding.getVedtakStatus().equals(KafkaVedtakStatusEndring.KafkaVedtakStatus.UTKAST_SLETTET)) {
                    vedtakStatusRepository.slettVedtakUtkast(melding);
                } else if(melding.getVedtakStatus().equals(KafkaVedtakStatusEndring.KafkaVedtakStatus.SENDT_TIL_BESLUTTER)) {
                    vedtakStatusRepository.slettVedtak(melding);
                    vedtakStatusRepository.upsertVedtak(melding);
                } else {
                    vedtakStatusRepository.upsertVedtak(melding);
                }
            }
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-vedtaksstotte-melding", t);
        }
    }
}
