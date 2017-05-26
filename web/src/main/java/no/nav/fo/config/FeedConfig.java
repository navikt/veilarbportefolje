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
import no.nav.fo.consumer.TilordningFeedHandler;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.service.OppdaterBrukerdataFletter;

@Configuration
public class FeedConfig {
    static {
        FeedConsumer.applicationApiroot = "veilarbportefolje/tjenester";
    }

    @Value("${dialogaktor.feed.producer.url}")
    private String dialogaktorHost;

    @Value("${dialogaktor.feed.consumer.pollingrate.cron}")
    private String dialogaktorPolling;

    // F.eks http://localhost:8486/veilarbsituasjon/api
    @Value("${tilordninger.feed.producer.url}")
    private String tilordningerHost;

    @Value("${tilordninger.feed.consumer.pollingrate.cron}")
    private String pollingRate;

    @Value("${tilordninger.feed.consumer.pollingratewebhook.cron}")
    private String pollingRateWebhook;

    @Bean
    public FeedController feedController(JdbcTemplate db, DialogDataFeedHandler callback) {
        FeedController feedController = new FeedController();

        feedController.addFeed("tilordninger", oppfolgingBrukerFeed());
        feedController.addFeed("dialogaktor", dialogDataFraFeedFeedConsumer(db, callback));

        return feedController;
    }

    private FeedConsumer<BrukerOppdatertInformasjon> oppfolgingBrukerFeed() {
        FeedConsumerConfig<BrukerOppdatertInformasjon> config = new FeedConsumerConfig<>(
                BrukerOppdatertInformasjon.class,
                "1970-01-01T00:00:00.000+02:00",
                tilordningerHost,
                "tilordninger"
        );

        config.pollingInterval(pollingRate);
        config.webhookPollingInterval(pollingRateWebhook);
        config.callback(page -> tilordningFeedHandler().handleFeedPage((page)));

        return new FeedConsumer<>(config);
    }

    private TilordningFeedHandler tilordningFeedHandler() {
        return new TilordningFeedHandler(oppdaterBrukerdataFletter());
    }


    @Bean
    public OppdaterBrukerdataFletter oppdaterBrukerdataFletter() {
        return new OppdaterBrukerdataFletter();
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
