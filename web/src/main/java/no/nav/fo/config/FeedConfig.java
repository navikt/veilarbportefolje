package no.nav.fo.config;

import no.nav.fo.domene.OppfolgingBruker;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.feed.controller.FeedController;
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

    @Bean
    public FeedController feedController() {
        FeedController feedController = new FeedController();

        FeedConsumer<OppfolgingBruker> consumer = oppfolgingBrukerFeed();
        feedController.addFeed("tilordninger", consumer);

        return feedController;
    }

    private FeedConsumer<OppfolgingBruker> oppfolgingBrukerFeed() {
        FeedConsumerConfig<OppfolgingBruker> config = new FeedConsumerConfig<>(
                OppfolgingBruker.class,
                "1970-01-01T00:00:00.000+02:00",
                tilordningerHost,
                "tilordninger"
        );

        return new FeedConsumer<>(config);
    }
}
