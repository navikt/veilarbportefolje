package no.nav.fo.config.feed;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.consumer.AktivitetFeedHandler;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.service.AktivitetService;
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
public class AktiviteterfeedConfig {

    @Value("${veilarbaktivitet.api.url}")
    private String host;

    @Value("${aktiviteter.feed.pagesize: 500}")
    private int pageSize;

    @Inject
    private DataSource dataSource;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    @Bean
    public FeedConsumer<AktivitetDataFraFeed> aktivitetDataFraFeedFeedConsumer(JdbcTemplate db, AktivitetFeedHandler callback, AktivitetDAO aktivitetDAO) {
        BaseConfig<AktivitetDataFraFeed> baseConfig = new BaseConfig<>(
                AktivitetDataFraFeed.class,
                Utils.apply(AktiviteterfeedConfig::sisteEndring, aktivitetDAO),
                host,
                "aktiviteter"
        );

        FeedConsumerConfig<AktivitetDataFraFeed> config = new FeedConsumerConfig<>(baseConfig, new FeedConsumerConfig.SimplePollingConfig(10))
                .callback(callback)
                .pageSize(pageSize)
                .lockProvider(lockProvider(dataSource), 5)
                .interceptors(singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }

    @Bean
    public AktivitetFeedHandler aktivitetFeedHandler(BrukerRepository brukerRepository,
                                                     AktivitetService aktivitetService,
                                                     AktoerService aktoerService,
                                                     SolrService solrService,
                                                     AktivitetDAO aktivitetDAO) {
        return new AktivitetFeedHandler(brukerRepository, aktivitetService, aktoerService, solrService, aktivitetDAO);
    }

    private static String sisteEndring(AktivitetDAO aktivitetDAO) {
        Timestamp sisteEndring = aktivitetDAO.getAktiviteterSistOppdatert();
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }
}
