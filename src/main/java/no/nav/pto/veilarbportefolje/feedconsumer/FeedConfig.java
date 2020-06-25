package no.nav.pto.veilarbportefolje.feedconsumer;

import no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktiviteterfeedConfig;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.feedconsumer.aktivitet.AktivitetDataFraFeed;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedConsumer;
import no.nav.pto.veilarbportefolje.feed.controller.FeedController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        OppfolgingerfeedConfig.class,
        AktiviteterfeedConfig.class
})
public class FeedConfig {

    public static String FEED_API_ROOT = "veilarbportefolje/api";
    public static final int FEED_PAGE_SIZE = 999;
    public static final int FEED_POLLING_INTERVAL_IN_SECONDS = 10;

    @Bean
    public FeedController feedController(
            FeedConsumer<BrukerOppdatertInformasjon> brukerOppdatertInformasjonFeedConsumer,
            FeedConsumer<AktivitetDataFraFeed> aktivitetDataFraFeedFeedConsumer
    ) {
        FeedController feedController = new FeedController();

        feedController.addFeed(BrukerOppdatertInformasjon.FEED_NAME, brukerOppdatertInformasjonFeedConsumer);
        feedController.addFeed(AktivitetDataFraFeed.FEED_NAME, aktivitetDataFraFeedFeedConsumer);

        return feedController;
    }
}
