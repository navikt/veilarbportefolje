package no.nav.pto.veilarbportefolje.oppfolging;

import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.metrics.MetricsClient;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.kafka.KafkaConfig;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerRunnable;
import no.nav.pto.veilarbportefolje.kafka.KafkaConsumerService;
import no.nav.pto.veilarbportefolje.util.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class OppfolgingConfig {

    @Bean
    public OppfolgingRepository oppfolgingRepository(JdbcTemplate db){
        return new OppfolgingRepository(db);
    }

    @Bean
    public KafkaConsumerRunnable kafkaOppfolgingConsumer(KafkaConsumerService oppfolgingService, UnleashService unleashService) {
        return new KafkaConsumerRunnable<>(
                oppfolgingService,
                unleashService,
                KafkaProperties.kafkaProperties(),
                KafkaConfig.Topic.OPPFOLGING_CONSUMER_TOPIC,
                FeatureToggle.KAFKA_OPPFOLGING
        );
    }

    @Bean
    public KafkaConsumerService oppfolgingService(
            OppfolgingRepository oppfolgingRepository,
            ElasticIndexer elasticIndexer,
            VeilarbVeilederClient veilarbVeilederClient,
            NavKontorService navKontorService,
            ArbeidslisteService arbeidslisteService,
            UnleashService unleashService,
            AktorregisterClient aktorregisterClient,
            MetricsClient metricsClient){
        return new OppfolgingService(oppfolgingRepository,
                elasticIndexer,
                veilarbVeilederClient,
                navKontorService,
                arbeidslisteService,
                unleashService,
                aktorregisterClient,
                metricsClient
        );
    }
}
