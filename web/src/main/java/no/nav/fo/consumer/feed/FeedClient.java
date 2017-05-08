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
    private final static MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final static String HTTPS = "https";

    @Value("${feed.producer.host}")
    private String feedProducerHost;

    @Value("${feed.producer.path}")
    private String feedProducerPath;

    @Value("${feed.producer.path.webhook}")
    private String feedProducerPathWebhook;

    @Value("${feed.consumer.url.callback}")
    private String feedConsumerUrlCallback;

    @Timed(name = "feed.registerWebhook")
    @Scheduled(cron = "${feed.consumer.pollingrate.cron}")
    public void registerWebhook() {
        String json = String.format("\"callbackUrl\": \"%s\"", feedConsumerUrlCallback);

        OkHttpClient client = new OkHttpClient();

        HttpUrl webhookUrl = new HttpUrl.Builder()
                .scheme(HTTPS)
                .host(feedProducerHost)
                .port(8485)
                .addPathSegments(feedProducerPathWebhook)
                .build();

        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder().url(webhookUrl).post(body).build();

        Try.of(() -> {
            Response response = client.newCall(request).execute();
            if (response.code() == 201) {
                LOG.info("Webhook opprettet hos produsent!");
            } else if (isNotSuccessful(response)) {
                LOG.warn("Produsent returnerte feilkode ", response.code());
            }
            return response;
        }).onFailure(e -> LOG.warn("Kunne ikke opprette webhook: {}.", e.getMessage()));
    }

    @Timed(name = "feed.poll")
    @Scheduled(cron = "${feed.consumer.pollingrate.cron}")
    public void pollFeed() {

        OkHttpClient client = new OkHttpClient();

        HttpUrl feedUrl = new HttpUrl.Builder()
                .scheme(HTTPS)
                .host(feedProducerHost)
                .port(8485)
                .addPathSegments(feedProducerPath)
                .addQueryParameter("since_id", "2017-01-01T00:00:00Z")
                .build();

        Request request = new Request.Builder().url(feedUrl).build();

        Try.of(() -> {
            Response response = client.newCall(request).execute();
            if (isNotSuccessful(response)) {
                LOG.warn("Produsent returnerte feilkode ", response.code());
            }
            return response;
        }).onFailure(e -> LOG.warn("Det skjedde en feil ved polling av feed: {}", e.getMessage()));

    }

    private boolean isNotSuccessful(Response response) {
        return !response.isSuccessful();
    }
}
