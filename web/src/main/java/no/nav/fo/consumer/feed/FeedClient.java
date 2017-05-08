package no.nav.fo.consumer.feed;

import javaslang.control.Try;
import no.nav.metrics.aspects.Timed;
import okhttp3.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import static org.slf4j.LoggerFactory.getLogger;

public class FeedClient {

    private static final Logger LOG = getLogger(FeedClient.class);
    private final static MediaType TEXT = MediaType.parse("application/text; charset=utf-8");
    private final static String HTTPS = "https";
    private static final String HTTP = "http";
    private final int port = 8486;

    @Value("${feed.producer.host}")
    private String feedProducerHost;

    @Value("${feed.producer.path}")
    private String feedProducerPath;

    @Value("${feed.producer.path.webhook}")
    private String feedProducerPathWebhook;

    @Value("${feed.consumer.url.callback}")
    private String feedConsumerUrlCallback;

    public void callback() {
        LOG.info("Webhook was activated!");
        this.pollFeed();
    }

    @Timed(name = "feed.registerWebhook")
    @Scheduled(cron = "${feed.consumer.pollingrate.cron}")
    public void registerWebhook() {

        OkHttpClient client = new OkHttpClient();

        HttpUrl webhookUrl = new HttpUrl.Builder()
                .scheme(HTTP)
                .host(feedProducerHost)
                .port(port)
                .addPathSegments(feedProducerPathWebhook)
                .build();

        RequestBody body = RequestBody.create(TEXT, feedConsumerUrlCallback);
        Request request = new Request.Builder().url(webhookUrl).put(body).build();

        Try.of(() -> {
            Response response = client.newCall(request).execute();
            if (response.code() == 201) {
                LOG.info("Webhook opprettet hos produsent!");
            } else if (isNotSuccessful(response)) {
                LOG.warn("Endepunkt for opprettelse av webhook returnerte feilkode {}: {}", response.code(), response.message());
            }
            LOG.debug("Pollet webhook: {}", response.code());
            return response;
        }).onFailure(e -> LOG.warn("Kunne ikke opprette webhook: {}.", e.getMessage()));
    }

    @Timed(name = "feed.poll")
    @Scheduled(cron = "${feed.consumer.pollingrate.cron}")
    public void pollFeed() {

        OkHttpClient client = new OkHttpClient();

        HttpUrl feedUrl = new HttpUrl.Builder()
                .scheme(HTTP)
                .host(feedProducerHost)
                .port(port)
                .addPathSegments(feedProducerPath)
                .addQueryParameter("since_id", "2017-01-01T00:00:00Z")
                .build();

        Request request = new Request.Builder().url(feedUrl).build();

        Try.of(() -> {
            Response response = client.newCall(request).execute();
            if (isNotSuccessful(response)) {
                LOG.warn("Endepunkt for polling av feed returnerte feilkode {}: {}", response.code(), response.message());
            }
            LOG.info("Pollet feed. Produsent svarte med {}", response.code());
            LOG.info("{}", response.body().string());
            return response;
        }).onFailure(e -> LOG.warn("Det skjedde en feil ved polling av feed: {}", e.getMessage()));

    }

    private boolean isNotSuccessful(Response response) {
        return !response.isSuccessful();
    }
}
