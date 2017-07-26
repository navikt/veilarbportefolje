package no.nav.fo.config.feed;

import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.consumer.AktivitetFeedHandler;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.service.AktivitetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.util.Collections.singletonList;
import static no.nav.fo.feed.consumer.FeedConsumerConfig.*;


@Configuration
public class AktiviteterfeedConfig {
    @Value("${aktiviteter.feed.isalive.url}")
    private String isaliveUrl;

    @Value("${aktiviteter.feed.producer.url}")
    private String host;

    @Value("${aktiviteter.feed.consumer.pollingrate.cron}")
    private String polling;

    @Value("${dialogaktor.feed.pagesize ?: 500}")
    private int pageSize;

    @Bean
    public FeedConsumer<AktivitetDataFraFeed> aktivitetDataFraFeedFeedConsumer(JdbcTemplate db, AktivitetFeedHandler callback, BrukerRepository brukerRepository) {
        BaseConfig<AktivitetDataFraFeed> baseConfig = new BaseConfig<>(
                AktivitetDataFraFeed.class,
                Utils.apply(AktiviteterfeedConfig::sisteEndring, brukerRepository),
                host,
                "aktiviteter"
        );

        FeedConsumerConfig<AktivitetDataFraFeed> config = new FeedConsumerConfig<>(baseConfig, new PollingConfig(polling))
                .callback(callback)
                .pageSize(pageSize)
                .interceptors(singletonList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }

    @Bean
    public AktivitetFeedHandler aktivitetFeedHandler(BrukerRepository brukerRepository, AktivitetService aktivitetService) {
        return new AktivitetFeedHandler(brukerRepository, aktivitetService);
    }

    private static String sisteEndring(BrukerRepository brukerRepository) {
        Timestamp sisteEndring = brukerRepository.getAktiviteterSistOppdatert();
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }
}
