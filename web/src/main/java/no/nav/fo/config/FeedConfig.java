package no.nav.fo.config;

import no.nav.fo.domene.OppfolgingBruker;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.controller.FeedController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeedConfig {
    static {
        FeedConsumer.applicationContextroot = "tjenester";
    }

    // F.eks http://localhost:8486/veilarbsituasjon/api
    @Value("${tilordninger.feed.producer.url}")
    private String tilordningerHost;

    @Bean
    public FeedController feedController(FeedConsumer<String, OppfolgingBruker> oppfolgingBrukerFeed) {
        FeedController feedController = new FeedController();

        feedController.addFeed("tilordninger", oppfolgingBrukerFeed);

        oppfolgingBrukerFeed
                .addCallback(data -> {
                    System.out.println(data);
                });

        return feedController;
    }

    @Bean
    public FeedConsumer<String, OppfolgingBruker> oppfolgingBrukerFeed() {
        return FeedConsumer.<String, OppfolgingBruker>builder()
                .host(tilordningerHost)
                .allowWebhooks(true)
                .feedName("tilordninger")
                .build();
    }
}
