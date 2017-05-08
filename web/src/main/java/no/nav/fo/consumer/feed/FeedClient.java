package no.nav.fo.consumer.feed;

import javaslang.control.Try;
import okhttp3.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import static org.slf4j.LoggerFactory.getLogger;

public class FeedClient {

    private static final Logger LOG = getLogger(FeedClient.class);

    private final static MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    @Value("${feed.producer.path.webhook}")
    private String feedProducerPathWebhook;

    @Value("${feed.consumer.path.callback}")
    private String feedConsumerPathCallback;

    @Scheduled(cron="${feed.consumer.pollingrate.cron}")
    public void registerWebhook() {
        String json = String.format("\"callbackUrl\": \"%s\"", feedConsumerPathCallback);
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder().url(feedProducerPathWebhook).post(body).build();

        Try.of(() -> {
            Response response = client.newCall(request).execute();
            if (response.code() == 201) {
                LOG.info("Webhook opprettet hos produsent!");
            } else if (isNotSuccessful(response)) {
                LOG.warn("Produsent returnerte feilkode ", response.code());
            }
            return response;
        }).onFailure(e -> LOG.warn("Kunne ikke opprette webhook. Prøv igjen senere"));
    }

    private boolean isNotSuccessful(Response response) {
        return !response.isSuccessful();
    }
}
