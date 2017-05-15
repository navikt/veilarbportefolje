package no.nav.fo.consumer;

import no.nav.fo.domene.Bruker;
import no.nav.fo.feed.consumer.FeedConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import javax.ws.rs.core.Response;

public class TilordningFeedClient extends FeedConsumer<Bruker> {

    private final FeedConsumer feed;

    @Value("${feed.producer.url}")
    private String feedProducerUrl;

    @Value("${feed.producer.url.registerwebhook}")
    private String feedproducerUrlWebhook;

    @Value("${feed.consumer.url.callback}")
    private String feedConsumerUrlCallback;

    public TilordningFeedClient() {
        feed = new FeedConsumer()
                .setFeedProducerUrl(feedProducerUrl)
                .setFeedProducerUrlWebhook(feedproducerUrlWebhook)
                .setOnWebhook(this::poll);
    }

    @Scheduled(cron = "${feed.consumer.pollingrate.cron}")
    public  void poll() {
        String sinceId = "2017-01-01T00:00:00Z";
        Response response = feed.poll(sinceId, 1000);
    }

    @Scheduled(cron = "${feed.consumer.pollingrate.cron}")
    public void registerWebhook() {
        feed.registerWebhook(feedConsumerUrlCallback);
    }
}
