package no.nav.pto.veilarbportefolje.kafka;

import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaConsumer<String, String> kafkaConsumer() {
        return new KafkaConsumer<>(KafkaProperties.kafkaProperties());
    }

    @Bean
    public KafkaConsumerRunnable kafkaConsumerRunnable(DialogService dialogService, VedtakService vedtakService, UnleashService unleashService, Consumer<String, String> kafkaConsumer) {
        return new KafkaConsumerRunnable(dialogService, vedtakService, unleashService, kafkaConsumer);
    }

}
