package no.nav.fo.consumer.feed;

import javaslang.control.Try;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import static org.slf4j.LoggerFactory.getLogger;

public class FeedClient {

    private static final Logger LOG = getLogger(FeedClient.class);

    private final static MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    @Value("${feed.producer.path.webhook}")
    private String feedProducerPathWebhook;

    @Value("${feed.consumer.path.callback}")
    private String feedConsumerPathCallback;

    public void registerWebhook() {
        String json = String.format("\"callbackUrl\": \"%s\"", feedConsumerPathCallback);
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder().url(feedProducerPathWebhook).post(body).build();

        Try.of(() -> client.newCall(request).execute())
                .onFailure(e -> LOG.warn("Kunne ikke oprette webhook. Prøv igjen senere"));
    }
}
