package no.nav.pto.veilarbportefolje.kafka;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.utils.EnvironmentUtils;
import no.nav.pto.veilarbportefolje.aktviteter.KafkaAktivitetService;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingService;
import no.nav.pto.veilarbportefolje.profilering.ProfileringService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Configuration
public class KafkaConfig {

    public final static String KAFKA_OPPFOLGING_TOGGLE = "portefolje.kafka.oppfolging";
    public final static String KAFKA_OPPFOLGING_BEHANDLE_MELDINGER_TOGGLE = "portefolje.kafka.oppfolging_behandle_meldinger";

    public enum Topic {
        VEDTAK_STATUS_ENDRING_TOPIC("aapen-oppfolging-vedtakStatusEndring-v1-" + EnvironmentUtils.getNamespace()),
        DIALOG_CONSUMER_TOPIC("aapen-fo-endringPaaDialog-v1-" + EnvironmentUtils.getNamespace()),
        OPPFOLGING_CONSUMER_TOPIC("aapen-fo-endringPaaOppfolgingStatus-v1-" + EnvironmentUtils.getNamespace()),
        KAFKA_REGISTRERING_CONSUMER_TOPIC("aapen-arbeid-arbeidssoker-registrert-" + EnvironmentUtils.getNamespace()),
        KAFKA_AKTIVITER_CONSUMER_TOPIC("aapen-fo-endringPaaAktivitet-v1-" + EnvironmentUtils.getNamespace()),
        PAM_SAMTYKKE_ENDRET_V1("aapen-pam-samtykke-endret-v1"),
        KAFKA_PROFILERING_CONSUMER_TOPIC("aapen-arbeid-arbeidssoker-profilert-" + EnvironmentUtils.getNamespace());

        final String topic;

        Topic(String topic) {
            this.topic = topic;
        }
    }

    public static List<KafkaHelsesjekk> getHelseSjekker() {
        List<Topic> topics = Arrays.asList(Topic.values());
        return topics.stream().map(topic -> new KafkaHelsesjekk(topic)).collect(toList());
    }

    @Bean
    public KafkaConsumerRunnable kafkaConsumerRegistrering(RegistreringService registreringService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<>(
                registreringService,
                unleashService,
                KafkaProperties.kafkaMedAvroProperties(),
                Topic.KAFKA_REGISTRERING_CONSUMER_TOPIC,
                "veilarbportfolje.registrering");
    }

    @Bean
    public KafkaConsumerRunnable kafkaConsumerProfilering(ProfileringService profileringService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<>(
                profileringService,
                unleashService,
                KafkaProperties.kafkaMedAvroProperties(),
                Topic.KAFKA_PROFILERING_CONSUMER_TOPIC,
                "veilarbportfolje.profilering");
    }

    @Bean
    public KafkaConsumerRunnable kafkaAktivitetConsumer(KafkaAktivitetService kafkaAktivitetService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<>(
                kafkaAktivitetService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                Topic.KAFKA_AKTIVITER_CONSUMER_TOPIC,
                "portefolje.kafka.aktiviteter"
        );
    }

    @Bean
    public KafkaConsumerRunnable kafkaVedtakConsumer(VedtakService vedtakService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<>(
                vedtakService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                Topic.VEDTAK_STATUS_ENDRING_TOPIC,
                "veilarbportfolje-hent-data-fra-vedtakstotte"
        );
    }

    @Bean
    public KafkaConsumerRunnable kafkaOppfolgingConsumer(OppfolgingService oppfolgingService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<>(
                oppfolgingService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                Topic.OPPFOLGING_CONSUMER_TOPIC,
                KAFKA_OPPFOLGING_TOGGLE
        );
    }

    @Bean
    public KafkaConsumerRunnable kafkaDialogConsumer(DialogService dialogService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<>(
                dialogService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                KafkaConfig.Topic.DIALOG_CONSUMER_TOPIC,
                "veilarbdialog.kafka"
        );
    }

    @Bean
    public KafkaConsumerRunnable kafkaCvConsumer (CvService cvService, UnleashService unleashService){
        return new KafkaConsumerRunnable<>(
                cvService,
                unleashService,
                KafkaProperties.kafkaMedAvroProperties(),
                KafkaConfig.Topic.PAM_SAMTYKKE_ENDRET_V1,
                "veilarbportefolje.kafka.cv.killswitch"
        );
    }


}
