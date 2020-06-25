package no.nav.pto.veilarbportefolje.feedconsumer.aktivitet;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumer;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumerConfig;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.feedconsumer.OidcFeedOutInterceptor;
import no.nav.pto.veilarbportefolje.feedconsumer.Utils;
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
import static no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumerConfig.BaseConfig;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.VEILARBAKTIVITET_URL_PROPERTY;
import static no.nav.pto.veilarbportefolje.feedconsumer.FeedConfig.FEED_PAGE_SIZE;
import static no.nav.pto.veilarbportefolje.feedconsumer.FeedConfig.FEED_POLLING_INTERVAL_IN_SECONDS;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;


@Configuration
public class AktiviteterfeedConfig {

    @Inject
    private DataSource dataSource;

    private LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    @Bean
    public FeedConsumer<AktivitetDataFraFeed> aktivitetDataFraFeedFeedConsumer(JdbcTemplate db, AktivitetFeedHandler callback, AktivitetDAO aktivitetDAO) {
        BaseConfig<AktivitetDataFraFeed> baseConfig = new BaseConfig<>(
                AktivitetDataFraFeed.class,
                Utils.apply(AktiviteterfeedConfig::sisteEndring, aktivitetDAO),
                getRequiredProperty(VEILARBAKTIVITET_URL_PROPERTY),
                "aktiviteter"
        );

        FeedConsumerConfig<AktivitetDataFraFeed> config = new FeedConsumerConfig<>(baseConfig, new FeedConsumerConfig.SimplePollingConfig(FEED_POLLING_INTERVAL_IN_SECONDS))
                .callback(callback)
                .pageSize(FEED_PAGE_SIZE)
                .lockProvider(lockProvider(dataSource), 10000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }

    @Bean
    public AktivitetFeedHandler aktivitetFeedHandler(BrukerRepository brukerRepository,
                                                     AktivitetService aktivitetService,
                                                     UnleashService unleashService
                                                     ) {
        return new AktivitetFeedHandler(brukerRepository, aktivitetService, unleashService);
    }

    private static String sisteEndring(AktivitetDAO aktivitetDAO) {
        Timestamp sisteEndring = aktivitetDAO.getAktiviteterSistOppdatert();
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }
}
