package no.nav.pto.veilarbportefolje.kafka;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.profilering.ProfileringService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class KafkaConfig {

    public enum Topic {
        VEDTAK_STATUS_ENDRING_TOPIC(System.getenv("KAKFA_TOPIC_VEDTAK_STATUS_ENDRING")),
        DIALOG_CONSUMER_TOPIC(System.getenv("KAFKA_TOPIC_DIALOG")),
        OPPFOLGING_CONSUMER_TOPIC(System.getenv("KAFKA_TOPIC_OPPFOLGING")),
        KAFKA_REGISTRERING_CONSUMER_TOPIC(System.getenv("KAFKA_TOPIC_REGISTRERING")),
        KAFKA_AKTIVITER_CONSUMER_TOPIC(System.getenv("KAFKA_TOPIC_AKTIVITER")),
        PAM_SAMTYKKE_ENDRET_V1(System.getenv("KAFKA_TOPIC_CV_SAMTYKKE")),
        KAFKA_PROFILERING_CONSUMER_TOPIC(System.getenv("KAFKA_TOPIC_PROFILERING"));

        final String topic;

        Topic(String topic) {
            this.topic = topic;
        }
    }

    @Bean
    public KafkaConsumerRunnable kafkaConsumerRegistrering(RegistreringService registreringService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<>(
                registreringService,
                unleashService,
                KafkaProperties.kafkaMedAvroProperties(),
                Topic.KAFKA_REGISTRERING_CONSUMER_TOPIC,
                FeatureToggle.KAFKA_REGISTRERING);
    }

    @Bean
    public KafkaConsumerRunnable kafkaConsumerProfilering(ProfileringService profileringService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<>(
                profileringService,
                unleashService,
                KafkaProperties.kafkaMedAvroProperties(),
                Topic.KAFKA_PROFILERING_CONSUMER_TOPIC,
                FeatureToggle.KAFKA_PROFILERING);
    }

    @Bean
    public KafkaConsumerRunnable kafkaAktivitetConsumer(AktivitetService aktivitetService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<>(
                aktivitetService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                Topic.KAFKA_AKTIVITER_CONSUMER_TOPIC,
                FeatureToggle.KAFKA_AKTIVITETER
        );
    }

    @Bean
    public KafkaConsumerRunnable kafkaVedtakConsumer(VedtakService vedtakService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<>(
                vedtakService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                Topic.VEDTAK_STATUS_ENDRING_TOPIC,
                FeatureToggle.KAFKA_VEDTAKSTOTTE
        );
    }

    @Bean
    public KafkaConsumerRunnable kafkaDialogConsumer(DialogService dialogService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<>(
                dialogService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                KafkaConfig.Topic.DIALOG_CONSUMER_TOPIC,
                FeatureToggle.KAFKA_VEILARBDIALOG
        );
    }

    @Bean
    public KafkaConsumerRunnable kafkaCvConsumer (CvService cvService, UnleashService unleashService){
        return new KafkaConsumerRunnable<>(
                cvService,
                unleashService,
                KafkaProperties.kafkaMedAvroProperties(),
                KafkaConfig.Topic.PAM_SAMTYKKE_ENDRET_V1,
                FeatureToggle.KAFKA_CV
        );
    }
}
