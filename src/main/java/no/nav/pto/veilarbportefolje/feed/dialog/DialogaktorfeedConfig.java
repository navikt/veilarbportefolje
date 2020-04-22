package no.nav.pto.veilarbportefolje.feed.dialog;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.feed.consumer.FeedConsumerConfig.SimplePollingConfig;
import no.nav.pto.veilarbportefolje.feed.Utils;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.AktoerService;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.util.Collections.singletonList;
import static no.nav.fo.feed.consumer.FeedConsumerConfig.BaseConfig;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.VEILARBDIALOG_URL_PROPERTY;
import static no.nav.pto.veilarbportefolje.feed.FeedConfig.FEED_PAGE_SIZE;
import static no.nav.pto.veilarbportefolje.feed.FeedConfig.FEED_POLLING_INTERVAL_IN_SECONDS;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;

@Configuration
public class DialogaktorfeedConfig {

    @Inject
    private DataSource dataSource;

    private LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    @Bean
    public FeedConsumer<DialogDataFraFeed> dialogDataFraFeedFeedConsumer(JdbcTemplate db, FeedCallback<DialogDataFraFeed> callback) {
        BaseConfig<DialogDataFraFeed> baseConfig = new BaseConfig<>(
                DialogDataFraFeed.class,
                Utils.apply(DialogaktorfeedConfig::sisteEndring, db),
                getRequiredProperty(VEILARBDIALOG_URL_PROPERTY),
                "dialogaktor"
        );

        FeedConsumerConfig<DialogDataFraFeed> config = new FeedConsumerConfig<>(baseConfig, new SimplePollingConfig(FEED_POLLING_INTERVAL_IN_SECONDS))
                .callback(callback)
                .pageSize(FEED_PAGE_SIZE)
                .lockProvider(lockProvider(dataSource), 10000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }

    private static String sisteEndring(JdbcTemplate db) {
        Timestamp sisteEndring = (Timestamp) db.queryForList("SELECT dialogaktor_sist_oppdatert FROM METADATA").get(0).get("dialogaktor_sist_oppdatert");
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }

    @Bean
    public FeedCallback<DialogDataFraFeed> dialogDataFeedHandler(AktoerService aktoerService,
                                                                 BrukerRepository brukerRepository,
                                                                 ElasticIndexer elasticIndexer,
                                                                 DialogFeedRepository dialogFeedRepository,
                                                                 UnleashService unleashService) {
        return new DialogDataFeedHandler(brukerRepository, elasticIndexer, dialogFeedRepository, unleashService);
    }
}
