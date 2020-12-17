package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.common.rest.client.RestClient;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.client.OidcInterceptor;
import no.nav.pto.veilarbportefolje.database.Transactor;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedCallback;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumer;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumerConfig;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static java.math.BigDecimal.valueOf;
import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.VEILARBOPPFOLGING_URL_PROPERTY;
import static no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumerConfig.*;

@Configuration
public class OppfolgingerfeedConfig {

    public static final String SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA = "SELECT oppfolging_sist_oppdatert_id FROM METADATA";
    public static String FEED_API_ROOT = "veilarbportefolje/api";
    public static final int FEED_PAGE_SIZE = 999;
    public static final int FEED_POLLING_INTERVAL_IN_SECONDS = 10;


    @Bean
    public FeedConsumer brukerOppdatertInformasjonFeedConsumer(
            JdbcTemplate db,
            SystemUserTokenProvider systemUserTokenProvider,
            FeedCallback callback) {
        BaseConfig<BrukerOppdatertInformasjon> baseConfig = new BaseConfig<>(
                BrukerOppdatertInformasjon.class,
                () -> nesteId(db),
                getRequiredProperty(VEILARBOPPFOLGING_URL_PROPERTY),
                BrukerOppdatertInformasjon.FEED_NAME
        );

        SimpleWebhookPollingConfig webhookPollingConfig = new SimpleWebhookPollingConfig(10, FEED_API_ROOT);

        OkHttpClient client = RestClient.baseClientBuilder().addInterceptor(new OidcInterceptor(systemUserTokenProvider)).build();

        FeedConsumerConfig config = new FeedConsumerConfig(baseConfig, new SimplePollingConfig(FEED_POLLING_INTERVAL_IN_SECONDS), webhookPollingConfig)
                .callback(callback)
                .pageSize(FEED_PAGE_SIZE)
                .restClient(client)
                .authorizatioModule(new OidcFeedAuthorizationModule());
        return new FeedConsumer(config);
    }

    @Bean
    public FeedCallback oppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                              BrukerService brukerService,
                                              ElasticIndexer elasticIndexer,
                                              OppfolgingRepository oppfolgingRepository,
                                              Transactor transactor,
                                              LeaderElectionClient leaderElectionClient,
                                              UnleashService unleashService) {
        return new OppfolgingFeedHandler(
                arbeidslisteService,
                brukerService,
                elasticIndexer,
                oppfolgingRepository,
                transactor,
                leaderElectionClient,
                unleashService);
    }

    public static String nesteId(JdbcTemplate db) {
        return ((BigDecimal) db.queryForList(SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA).stream()
                .findFirst()
                .map(m -> m.get("oppfolging_sist_oppdatert_id"))
                .orElse(valueOf(0)))
                .add(valueOf(1))
                .toPlainString();
    }
}
