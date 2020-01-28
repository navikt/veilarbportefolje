package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.database.VedtakStatusRepository;
import no.nav.pto.veilarbportefolje.domene.KafkaVedtakStatusEndring;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;

import static no.nav.sbl.util.EnvironmentUtils.requireEnvironmentName;
import static no.nav.sbl.util.EnvironmentUtils.Type.PUBLIC;
import static no.nav.sbl.util.EnvironmentUtils.setProperty;
import static no.nav.sbl.util.EnvironmentUtils.getOptionalProperty;
import static no.nav.json.JsonUtils.fromJson;

@Slf4j
public class VedtaksstotteConsumer {

    private static final String KAFKA_CONSUMER_TOPIC = "aapen-oppfolging-vedtakStatusEndring-v1" + requireEnvironmentName();
    private static final String ENDRING_PAA_VEDTAKSTATUS_KAFKA_TOPIC_PROPERTY_NAME = "ENDRING_PAA_AVSLUTTOPPFOLGING_TOPIC";
    private VedtakStatusRepository vedtakStatusRepository;

    public VedtaksstotteConsumer(VedtakStatusRepository vedtakStatusRepository) {
        this.vedtakStatusRepository = vedtakStatusRepository;
        setProperty(ENDRING_PAA_VEDTAKSTATUS_KAFKA_TOPIC_PROPERTY_NAME, KAFKA_CONSUMER_TOPIC, PUBLIC);
    }

    @KafkaListener(topics = "${" + ENDRING_PAA_VEDTAKSTATUS_KAFKA_TOPIC_PROPERTY_NAME + "}")
    public void consume(@Payload String kafkaMelding) {
        try {
            KafkaVedtakStatusEndring melding = fromJson(kafkaMelding, KafkaVedtakStatusEndring.class);
            log.info("Leser melding for aktorId:" + melding.getAktorId() + " på topic: " + getOptionalProperty(ENDRING_PAA_VEDTAKSTATUS_KAFKA_TOPIC_PROPERTY_NAME));
            vedtakStatusRepository.upsertVedtak(melding);
        } catch (Throwable t) {
            log.error("Feilet ved behandling av kafka-vedtaksstotte-melding", t);
        }
    }
}
