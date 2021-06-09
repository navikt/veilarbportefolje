package no.nav.pto.veilarbportefolje.kafka;

import no.nav.arbeid.soker.profilering.ArbeidssokerProfilertEvent;
import no.nav.arbeid.soker.registrering.ArbeidssokerRegistrertEvent;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetService;
import no.nav.pto.veilarbportefolje.cv.CVService;
import no.nav.pto.veilarbportefolje.dialog.DialogService;
import no.nav.pto.veilarbportefolje.mal.MalService;
import no.nav.pto.veilarbportefolje.oppfolging.*;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolginsbrukerService;
import no.nav.pto.veilarbportefolje.profilering.ProfileringService;
import no.nav.pto.veilarbportefolje.registrering.RegistreringService;
import no.nav.pto.veilarbportefolje.sistelest.SistLestService;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import no.nav.pto.veilarbportefolje.util.KafkaProperties.KafkaAutoOffset;
import no.nav.pto.veilarbportefolje.vedtakstotte.VedtakService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;


@Configuration
public class KafkaConfig {

    public enum Topic {
        VEDTAK_STATUS_ENDRING_TOPIC("aapen-oppfolging-vedtakStatusEndring-v1-" + requireKafkaTopicPostfix()),
        DIALOG_CONSUMER_TOPIC("aapen-fo-endringPaaDialog-v1-" + requireKafkaTopicPostfix()),
        KAFKA_REGISTRERING_CONSUMER_TOPIC("aapen-arbeid-arbeidssoker-registrert-" + requireKafkaTopicPostfix()),
        KAFKA_AKTIVITER_CONSUMER_TOPIC("aapen-fo-endringPaaAktivitet-v4-" + requireKafkaTopicPostfix()),
        KAFKA_PROFILERING_CONSUMER_TOPIC("aapen-arbeid-arbeidssoker-profilert-" + requireKafkaTopicPostfix()),
        ENDRING_PAA_MANUELL_STATUS("aapen-arbeidsrettetOppfolging-endringPaManuellStatus-v1-" + requireKafkaTopicPostfix()),
        VEILEDER_TILORDNET("aapen-arbeidsrettetOppfolging-veilederTilordnet-v1-" + requireKafkaTopicPostfix()),
        ENDRING_PAA_NY_FOR_VEILEDER("aapen-arbeidsrettetOppfolging-endringPaNyForVeileder-v1-" + requireKafkaTopicPostfix()),
        OPPFOLGING_STARTET("aapen-arbeidsrettetOppfolging-oppfolgingStartet-v1-" + requireKafkaTopicPostfix()),
        OPPFOLGING_AVSLUTTET("aapen-arbeidsrettetOppfolging-oppfolgingAvsluttet-v1-" + requireKafkaTopicPostfix()),
        ENDRING_PA_MAL("aapen-arbeidsrettetOppfolging-endringPaMal-v1-" + requireKafkaTopicPostfix()),
        SIST_LEST("aapen-fo-veilederHarLestAktivitetsplanen-v1"),
        ENDRING_PAA_OPPFOLGINGSBRUKER("aapen-fo-endringPaaOppfoelgingsBruker-v1-" + requireKafkaTopicPostfix()),
        CV_ENDRET("arbeid-pam-cv-endret-v6");

        final String topicName;

        Topic(String topicName) {
            this.topicName = topicName;
        }
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaConsumerSistLest(SistLestService sistLestService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                sistLestService,
                unleashService,
                KafkaProperties.kafkaProperties(KafkaAutoOffset.EARLIEST),
                Topic.SIST_LEST,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<ArbeidssokerRegistrertEvent> kafkaConsumerRegistrering(RegistreringService registreringService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                registreringService,
                unleashService,
                KafkaProperties.kafkaMedAvroProperties(KafkaAutoOffset.NONE),
                Topic.KAFKA_REGISTRERING_CONSUMER_TOPIC,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<ArbeidssokerProfilertEvent> kafkaConsumerProfilering(ProfileringService profileringService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                profileringService,
                unleashService,
                KafkaProperties.kafkaMedAvroProperties(KafkaAutoOffset.NONE),
                Topic.KAFKA_PROFILERING_CONSUMER_TOPIC,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaAktivitetConsumer(AktivitetService aktivitetService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                aktivitetService,
                unleashService,
                KafkaProperties.kafkaProperties(KafkaAutoOffset.NONE),
                Topic.KAFKA_AKTIVITER_CONSUMER_TOPIC,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaVedtakConsumer(VedtakService vedtakService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                vedtakService,
                unleashService,
                KafkaProperties.kafkaProperties(KafkaAutoOffset.NONE),
                Topic.VEDTAK_STATUS_ENDRING_TOPIC,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaDialogConsumer(DialogService dialogService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                dialogService,
                unleashService,
                KafkaProperties.kafkaProperties(KafkaAutoOffset.NONE),
                KafkaConfig.Topic.DIALOG_CONSUMER_TOPIC,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaOppfolgingStartet(OppfolgingStartetService oppfolgingStartetService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                oppfolgingStartetService,
                unleashService,
                KafkaProperties.kafkaProperties(KafkaAutoOffset.NONE),
                Topic.OPPFOLGING_STARTET,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaOppfolgingAvsluttet(OppfolgingAvsluttetService oppfolgingAvsluttetService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                oppfolgingAvsluttetService,
                unleashService,
                KafkaProperties.kafkaProperties(KafkaAutoOffset.NONE),
                KafkaConfig.Topic.OPPFOLGING_AVSLUTTET,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaEndringPaaManuellStatus(ManuellStatusService manuellStatusService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                manuellStatusService,
                unleashService,
                KafkaProperties.kafkaProperties(KafkaAutoOffset.NONE),
                Topic.ENDRING_PAA_MANUELL_STATUS,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaEndringPaaNyForVeileder(NyForVeilederService nyForVeilederService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                nyForVeilederService,
                unleashService,
                KafkaProperties.kafkaProperties(KafkaAutoOffset.NONE),
                Topic.ENDRING_PAA_NY_FOR_VEILEDER,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaVeilederTilordnet(VeilederTilordnetService veilederTilordnetService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                veilederTilordnetService,
                unleashService,
                KafkaProperties.kafkaProperties(KafkaAutoOffset.NONE),
                Topic.VEILEDER_TILORDNET,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaConsumerMal(MalService malService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                malService,
                unleashService,
                KafkaProperties.kafkaProperties(KafkaAutoOffset.NONE),
                Topic.ENDRING_PA_MAL,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaEndringOppfolgingsbruker(OppfolginsbrukerService oppfolginsbrukerService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                oppfolginsbrukerService,
                unleashService,
                KafkaProperties.kafkaProperties(KafkaAutoOffset.EARLIEST),
                Topic.ENDRING_PAA_OPPFOLGINGSBRUKER,
                metricsClient
        );
    }

    @Bean
    public KafkaConsumerRunnable<String> kafkaEndringCV(CVService cvService, UnleashService unleashService, MetricsClient metricsClient) {
        return new KafkaConsumerRunnable<>(
                cvService,
                unleashService,
                KafkaProperties.kafkaProperties(KafkaAutoOffset.EARLIEST),
                Topic.CV_ENDRET,
                metricsClient
        );
    }


    public static String requireKafkaTopicPostfix() {
        return isDevelopment().orElse(false) ? "q1" : "p";
    }
}
