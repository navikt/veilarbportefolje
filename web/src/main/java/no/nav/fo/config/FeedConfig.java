package no.nav.fo.config;

import no.nav.fo.consumer.TilordningFeedHandler;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeedConfig {
    static {
        FeedConsumer.applicationApiroot = "veilarbportefolje/tjenester";
    }

    // F.eks http://localhost:8486/veilarbsituasjon/api
    @Value("${tilordninger.feed.producer.url}")
    private String tilordningerHost;

    @Value("${tilordninger.feed.consumer.pollingrate.cron}")
    private String pollingRate;

    @Value("${tilordninger.feed.consumer.pollingratewebhook.cron}")
    private String pollingRateWebhook;

    private FeedConsumer<BrukerOppdatertInformasjon> consumer;

    @Bean
    public FeedController feedController() {
        FeedController feedController = new FeedController();

        consumer = oppfolgingBrukerFeed();
        feedController.addFeed("tilordninger", consumer);

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
}
