package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.sts.SystemUserTokenProvider;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.client.OidcInterceptor;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedCallback;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumer;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumerConfig;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.sbl.jdbc.Transactor;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.math.BigDecimal;

import static java.math.BigDecimal.valueOf;
import static java.util.Collections.singletonList;
import static no.nav.common.utils.EnvironmentUtils.getRequiredProperty;
import static no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumerConfig.*;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.VEILARBOPPFOLGING_URL_PROPERTY;
import static no.nav.pto.veilarbportefolje.oppfolgingfeed.FeedConfig.*;


@Configuration
public class OppfolgingerfeedConfig {

    public static final String SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA = "SELECT oppfolging_sist_oppdatert_id FROM METADATA";
    private LockProvider lockProvider;

    @Autowired
    public OppfolgingerfeedConfig(DataSource dataSource) {
        this.lockProvider = new JdbcLockProvider(dataSource);
    }

    @Bean
    public FeedConsumer brukerOppdatertInformasjonFeedConsumer(
            JdbcTemplate db,
            SystemUserTokenProvider systemUserTokenProvider,
            OkHttpClient client,
            FeedCallback callback) {
        BaseConfig<BrukerOppdatertInformasjon> baseConfig = new BaseConfig<>(
                BrukerOppdatertInformasjon.class,
                () -> nesteId(db),
                getRequiredProperty(VEILARBOPPFOLGING_URL_PROPERTY),
                BrukerOppdatertInformasjon.FEED_NAME
        );

        SimpleWebhookPollingConfig webhookPollingConfig = new SimpleWebhookPollingConfig(10, FEED_API_ROOT);

        FeedConsumerConfig config = new FeedConsumerConfig(baseConfig, new SimplePollingConfig(FEED_POLLING_INTERVAL_IN_SECONDS), webhookPollingConfig)
                .callback(callback)
                .pageSize(FEED_PAGE_SIZE)
                .lockProvider(lockProvider, 10000)
                .interceptors(singletonList(new OidcInterceptor(systemUserTokenProvider)))
                .restClient(client)
                .authorizatioModule(new OidcFeedAuthorizationModule());
        return new FeedConsumer(config);
    }

    @Bean
    public FeedCallback oppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                              BrukerRepository brukerRepository,
                                              ElasticIndexer elasticIndexer,
                                              OppfolgingRepository oppfolgingRepository,
                                              VeilarbVeilederClient veilarbVeilederClient,
                                              Transactor transactor,
                                              UnleashService unleashService) {
        return new OppfolgingFeedHandler(
                arbeidslisteService,
                brukerRepository,
                elasticIndexer,
                oppfolgingRepository,
                veilarbVeilederClient,
                transactor,
                unleashService
        );
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
