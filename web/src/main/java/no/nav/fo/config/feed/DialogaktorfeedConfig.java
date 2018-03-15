package no.nav.fo.config.feed;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.consumer.DialogDataFeedHandler;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.feed.DialogFeedRepository;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.feed.consumer.FeedConsumerConfig.SimplePollingConfig;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.SolrService;
import org.springframework.beans.factory.annotation.Value;
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

@Configuration
public class DialogaktorfeedConfig {

    @Value("${veilarbdialog.api.url}")
    private String host;

    @Value("${dialogaktor.feed.pagesize:500}")
    private int pageSize;

    @Inject
    private DataSource dataSource;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    @Bean
    public FeedConsumer<DialogDataFraFeed> dialogDataFraFeedFeedConsumer(JdbcTemplate db, DialogDataFeedHandler callback) {
        BaseConfig<DialogDataFraFeed> baseConfig = new BaseConfig<>(
                DialogDataFraFeed.class,
                Utils.apply(DialogaktorfeedConfig::sisteEndring, db),
                host,
                "dialogaktor"
        );

        FeedConsumerConfig<DialogDataFraFeed> config = new FeedConsumerConfig<>(baseConfig, new SimplePollingConfig(10))
                .callback(callback)
                .pageSize(pageSize)
                .lockProvider(lockProvider(dataSource), 5)
                .interceptors(singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }

    private static String sisteEndring(JdbcTemplate db) {
        Timestamp sisteEndring = (Timestamp) db.queryForList("SELECT dialogaktor_sist_oppdatert FROM METADATA").get(0).get("dialogaktor_sist_oppdatert");
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }

    @Bean
    public DialogDataFeedHandler dialogDataFeedHandler(AktoerService aktoerService,
                                                       BrukerRepository brukerRepository,
                                                       SolrService solrService,
                                                       DialogFeedRepository dialogFeedRepository) {
        return new DialogDataFeedHandler(aktoerService, brukerRepository, solrService, dialogFeedRepository);
    }
}
