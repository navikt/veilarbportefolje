package no.nav.fo.config;

import no.nav.fo.consumer.AktivitetFeedHandler;
import no.nav.fo.domene.AktivitetDataFraFeed;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.feed.controller.FeedController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AktivitetFeedConfig {
    static {
        FeedConsumer.applicationApiroot = "veilarbportefolje/tjenester";
    }

    @Value("${aktiviteter.feed.producer.url}")
    private String aktiviteterHost;

    @Value("${aktiviteter.feed.consumer.pollingrate.cron}")
    private String pollingRate;

    @Value("${aktiviteter.feed.consumer.pollingratewebhook.cron}")
    private String pollingRateWebhook;

    private FeedConsumer<AktivitetDataFraFeed> consumer;

    @Bean
    public FeedController feedController() {
        FeedController feedController = new FeedController();

        consumer = aktiviteterFeed();
        feedController.addFeed("aktiviteter", consumer);

        return feedController;
    }

    private FeedConsumer<AktivitetDataFraFeed> aktiviteterFeed() {
        FeedConsumerConfig<AktivitetDataFraFeed> config = new FeedConsumerConfig<>(
                AktivitetDataFraFeed.class,
                "1970-01-01T00:00:00.000+02:00",
                aktiviteterHost,
                "aktiviteter"
        );

        config.pollingInterval(pollingRate);
        config.webhookPollingInterval(pollingRateWebhook);
        config.callback(page -> aktiviteterFeedHandler().handleFeedPage((page)));

        return new FeedConsumer<>(config);
    }

    private AktivitetFeedHandler aktiviteterFeedHandler() {
        return new AktivitetFeedHandler();
    }
}
