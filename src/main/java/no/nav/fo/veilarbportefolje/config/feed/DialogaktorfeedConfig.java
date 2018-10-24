package no.nav.fo.veilarbportefolje.config.feed;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.feed.consumer.FeedConsumerConfig.SimplePollingConfig;
import no.nav.fo.veilarbportefolje.consumer.DialogDataFeedHandler;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.domene.feed.DialogDataFraFeed;
import no.nav.fo.veilarbportefolje.feed.DialogFeedRepository;
import no.nav.fo.veilarbportefolje.service.AktoerService;
import no.nav.fo.veilarbportefolje.service.SolrService;
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
import static no.nav.fo.veilarbportefolje.config.ApplicationConfig.VEILARBDIALOG_URL_PROPERTY;
import static no.nav.fo.veilarbportefolje.config.FeedConfig.FEED_PAGE_SIZE;
import static no.nav.fo.veilarbportefolje.config.FeedConfig.FEED_POLLING_INTERVAL_IN_SECONDS;
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
                                                                 SolrService solrService,
                                                                 DialogFeedRepository dialogFeedRepository) {
        return new DialogDataFeedHandler(brukerRepository, solrService, dialogFeedRepository);
    }
}
