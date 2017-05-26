package no.nav.fo.config;

import no.nav.fo.consumer.DialogDataFeedHandler;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.feed.controller.FeedController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;

@Configuration
public class FeedConfig {
    static {
        FeedConsumer.applicationApiroot = "veilarbportefolje/tjenester";
    }

    @Value("${dialogaktor.feed.producer.url}")
    private String dialogaktorHost;

    @Value("${dialogaktor.feed.consumer.pollingrate.cron}")
    private String dialogaktorPolling;

    @Bean
    public FeedController feedController(JdbcTemplate db, DialogDataFeedHandler callback) {
        FeedController feedController = new FeedController();

        feedController.addFeed("dialogaktor", dialogDataFraFeedFeedConsumer(db, callback));

        return feedController;
    }

    private FeedConsumer<DialogDataFraFeed> dialogDataFraFeedFeedConsumer(JdbcTemplate db, DialogDataFeedHandler callback) {
        Timestamp sisteEndring = db.query("SELECT dialogaktor from METADATA", resultSet -> {
            return resultSet.getTimestamp("dialogaktor");
        });

        FeedConsumerConfig<DialogDataFraFeed> config = new FeedConsumerConfig<>(
                DialogDataFraFeed.class,
                sisteEndring.toString(),
                dialogaktorHost,
                "dialogaktor"
        )
                .pollingInterval(dialogaktorPolling)
                .callback(callback);

        return new FeedConsumer<>(config);
    }

    @Bean
    public DialogDataFeedHandler dialogDataFeedHandler() {
        return new DialogDataFeedHandler();
    }
}
