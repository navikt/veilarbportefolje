package no.nav.pto.veilarbportefolje.kafka;

import lombok.extern.slf4j.Slf4j;
import no.nav.apiapp.selftest.Helsesjekk;
import no.nav.apiapp.selftest.HelsesjekkMetadata;
import no.nav.jobutils.JobUtils;
import no.nav.pto.veilarbportefolje.vedtakstotte.KafkaVedtakStatusEndring;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;


import java.time.Duration;
import java.util.*;

import static no.nav.json.JsonUtils.fromJson;
import static no.nav.pto.veilarbportefolje.config.KafkaConfig.KAFKA_BROKERS;

@Slf4j
public class KafkaVedtakStotteConsumerRunnable implements Helsesjekk, Runnable {

    private VedtakService vedtakService;
    private UnleashService unleashService;


    private long lastThrownExceptionTime;
    private Exception e;
    private Consumer<String, String> kafkaVedtakStotteConsumer;




    public KafkaVedtakStotteConsumerRunnable(VedtakService vedtakService, UnleashService unleashService, Consumer<String, String> kafkaVedtakStotteConsumer) {
        // TODO SKA DENNA TA IN TOPICS ELLER SKA VI DEFINIERA ALLA TOPICS HER?
        // TODO SWITCH CASE PÅ TOPIC record.topic() ELLER LAGA EN NY INSTANSE AV DENNA KLASS FØR VARJE TOPIC ?
        this.kafkaVedtakStotteConsumer = kafkaVedtakStotteConsumer;

        this.vedtakService = vedtakService;
        this.unleashService = unleashService;

        JobUtils.runAsyncJob(this);
    }

    @Override
    public void run() {
        while (this.vedstakstotteFeatureErPa()) {
            try {
                ConsumerRecords<String, String> records = kafkaVedtakStotteConsumer.poll(Duration.ofSeconds(1L));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Behandler melding for på topic:" + record.topic());
                    KafkaVedtakStatusEndring melding = fromJson(record.value(), KafkaVedtakStatusEndring.class);
                    vedtakService.behandleMelding(melding);
                    kafkaVedtakStotteConsumer.commitSync();
                }
            }
            catch (Exception e) {
                this.e = e;
                this.lastThrownExceptionTime = System.currentTimeMillis();
                log.error("Feilet ved behandling av kafka-vedtaksstotte-melding", e);
            }
        }
    }

    @Override
    public void helsesjekk() {
        if ((this.lastThrownExceptionTime + 60_000L) > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Kafka vedtakstotteconsumer feilet " + new Date(this.lastThrownExceptionTime), this.e);
        }
    }

    @Override
    public HelsesjekkMetadata getMetadata() {
        return new HelsesjekkMetadata("kafka", KAFKA_BROKERS, "kafka", false);
    }

    private boolean vedstakstotteFeatureErPa () {
        return unleashService.isEnabled("veilarbportfolje-hent-data-fra-vedtakstotte");
    }

}
