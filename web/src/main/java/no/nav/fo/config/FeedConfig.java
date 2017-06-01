package no.nav.fo.config;

import no.nav.fo.config.feed.DialogaktorfeedConfig;
import no.nav.fo.config.feed.TilordningerfeedConfig;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.controller.FeedController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        DialogaktorfeedConfig.class,
        TilordningerfeedConfig.class
})
public class FeedConfig {
    static {
        FeedConsumer.applicationApiroot = "veilarbportefolje/tjenester";
    }

    @Bean
    public FeedController feedController(
            FeedConsumer<DialogDataFraFeed> dialogDataFraFeedFeedConsumer,
            FeedConsumer<BrukerOppdatertInformasjon> brukerOppdatertInformasjonFeedConsumer
    ) {
        FeedController feedController = new FeedController();

        feedController.addFeed("tilordninger", brukerOppdatertInformasjonFeedConsumer);
        feedController.addFeed("dialogaktor", dialogDataFraFeedFeedConsumer);

        return feedController;
    }
}
