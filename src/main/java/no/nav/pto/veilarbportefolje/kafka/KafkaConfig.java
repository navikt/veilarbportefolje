package no.nav.pto.veilarbportefolje.kafka;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.profilering.ProfileringService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.EnvironmentUtils.requireNamespace;


@Configuration
public class KafkaConfig {

    public enum Topic {
        VEDTAK_STATUS_ENDRING_TOPIC("aapen-oppfolging-vedtakStatusEndring-v1-" + requireKafkaTopicPostfix()),
        DIALOG_CONSUMER_TOPIC("aapen-fo-endringPaaDialog-v1-" + requireKafkaTopicPostfix()),
        KAFKA_REGISTRERING_CONSUMER_TOPIC("aapen-arbeid-arbeidssoker-registrert-" + requireKafkaTopicPostfix()),
        KAFKA_AKTIVITER_CONSUMER_TOPIC("aapen-fo-endringPaaAktivitet-v1-" + requireKafkaTopicPostfix()),
        PAM_SAMTYKKE_ENDRET_V1("aapen-pam-samtykke-endret-v1"),
        KAFKA_PROFILERING_CONSUMER_TOPIC("aapen-arbeid-arbeidssoker-profilert-" + requireKafkaTopicPostfix());

        final String topic;

        Topic(String topic) {
            this.topic = topic;
        }
    }

    @Bean
    public KafkaConsumerRunnable kafkaConsumerRegistrering(RegistreringService registreringService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                registreringService,
                unleashService,
                KafkaProperties.kafkaMedAvroProperties(),
                Topic.KAFKA_REGISTRERING_CONSUMER_TOPIC,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable kafkaConsumerProfilering(ProfileringService profileringService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                profileringService,
                unleashService,
                KafkaProperties.kafkaMedAvroProperties(),
                Topic.KAFKA_PROFILERING_CONSUMER_TOPIC,
                metricsClient);
    }

    @Bean
    public KafkaConsumerRunnable kafkaAktivitetConsumer(AktivitetService aktivitetService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                aktivitetService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                Topic.KAFKA_AKTIVITER_CONSUMER_TOPIC,
                metricsClient);
    }

    @Bean
    public KafkaConsumerRunnable kafkaVedtakConsumer(VedtakService vedtakService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                vedtakService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                Topic.VEDTAK_STATUS_ENDRING_TOPIC,
                metricsClient);
    }

    @Bean
    public KafkaConsumerRunnable kafkaDialogConsumer(DialogService dialogService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                dialogService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                KafkaConfig.Topic.DIALOG_CONSUMER_TOPIC,
                metricsClient);
    }

    @Bean
    public KafkaConsumerRunnable kafkaCvConsumer (CvService cvService, UnleashService unleashService, MetricsClient metricsClient){
        return new KafkaConsumerRunnable<>(
                cvService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                KafkaConfig.Topic.PAM_SAMTYKKE_ENDRET_V1,
                metricsClient);
    }

    public static String requireKafkaTopicPostfix() {
        String namespace = requireNamespace();
        return namespace.equals("default") ? "p" : namespace;
    }
}
