package no.nav.pto.veilarbportefolje.feed.consumer;

import lombok.SneakyThrows;
import net.javacrumbs.shedlock.core.LockConfiguration;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.pto.veilarbportefolje.feed.common.*;
import no.nav.pto.veilarbportefolje.feedconsumer.OidcFeedOutInterceptor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static no.nav.pto.veilarbportefolje.feed.consumer.FeedPoller.createScheduledJob;
import static no.nav.pto.veilarbportefolje.feed.util.UrlUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

public class FeedConsumer<DOMAINOBJECT extends Comparable<DOMAINOBJECT>> implements Pingable, Authorization, ApplicationListener<ContextClosedEvent> {
    private static final Logger LOG = getLogger(FeedConsumer.class);

    private final FeedConsumerConfig<DOMAINOBJECT> config;
    private final Ping.PingMetadata pingMetadata;
    private int lastResponseHash;

    private final OkHttpClient restClient;

    public FeedConsumer(FeedConsumerConfig<DOMAINOBJECT> config) {
        String feedName = config.feedName;
        String host = config.host;

        this.config = config;
        OkHttpClient.Builder clientBuilder = RestClient.baseClientBuilder();

        this.config.interceptors.forEach(clientBuilder::addInterceptor);

        this.restClient = clientBuilder.build();

        this.pingMetadata = new Ping.PingMetadata(getTargetUrl(), String.format("feed-consumer av '%s'", feedName), false);

        createScheduledJob(feedName, host, config.pollingConfig, runWithLock(feedName, this::poll));
        createScheduledJob(feedName + "/webhook", host, config.webhookPollingConfig, this::registerWebhook);
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        FeedPoller.shutdown();
    }

    public boolean webhookCallback() {
        if (this.config.webhookPollingConfig == null) {
            return false;
        }

        CompletableFuture.runAsync(runWithLock(this.config.feedName, this::poll));
        return true;
    }

    public void addCallback(FeedCallback callback) {
        this.config.callback(callback);
    }

    void registerWebhook() {
        String callbackUrl = callbackUrl(this.config.webhookPollingConfig.apiRootPath, this.config.feedName);
        FeedWebhookRequest body = new FeedWebhookRequest().setCallbackUrl(callbackUrl);

        Entity<FeedWebhookRequest> entity = Entity.entity(body, APPLICATION_JSON_TYPE);

        Invocation.Builder request = restClient
                .target(asUrl(this.config.host, "feed", this.config.feedName, "webhook"))
                .request();

        Response response = request
                .buildPut(entity)
                .invoke();

        int responseStatus = response.getStatus();
        if (responseStatus == 201) {
            LOG.info("Webhook opprettet hos produsent!");
        } else if (responseStatus != 200) {
            LOG.warn("Endepunkt for opprettelse av webhook returnerte feilkode {}", responseStatus);
        }
    }

    public synchronized Response poll() {
        Response response = fetchChanges();

        ParameterizedType type = new FeedParameterizedType(this.config.domainobject);
        FeedResponse<DOMAINOBJECT> entity = RestUtils.presponse.body(new GenericType<>(type));
        List<FeedElement<DOMAINOBJECT>> elements = entity.getElements();
        if (elements != null && !elements.isEmpty()) {
            List<DOMAINOBJECT> data = elements
                    .stream()
                    .map(FeedElement::getElement)
                    .collect(Collectors.toList());

            if (!(entity.hashCode() == lastResponseHash)) {
                this.config.callback.call(entity.getNextPageId(), data);
            }
            this.lastResponseHash = entity.hashCode();
        }


        return response;
    }

    @SneakyThrows
    Response fetchChanges() {
        String lastEntry = this.config.lastEntrySupplier.get();
        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(getTargetUrl())).newBuilder();
        httpBuilder.addQueryParameter(QUERY_PARAM_ID, lastEntry);
        httpBuilder.addQueryParameter(QUERY_PARAM_PAGE_SIZE, String.valueOf(this.config.pageSize));

        Request request = new Request.Builder().url(httpBuilder.build()).build();
        try (okhttp3.Response response = restClient.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return response;
        }
    }

    private String getTargetUrl() {
        return asUrl(this.config.host, "feed", this.config.feedName);
    }

    @Override
    public Ping ping() {
        try {
            int status = fetchChanges().getStatus();
            if (status == 200) {
                return Ping.lyktes(pingMetadata);
            } else {
                return Ping.feilet(pingMetadata, "HTTP status " + status);
            }
        } catch (Throwable e) {
            return Ping.feilet(pingMetadata, e);
        }
    }

    @Override
    public FeedAuthorizationModule getAuthorizationModule() {
        return config.authorizationModule;
    }

    private Runnable runWithLock(String lockname, Runnable task) {
        return () -> {
            if (this.config.lockExecutor == null) {
                task.run();
            } else {
                Instant lockAtMostUntil = Instant.now().plusMillis(this.config.lockHoldingLimitInMilliSeconds);
                LockConfiguration lockConfiguration = new LockConfiguration(lockname, lockAtMostUntil);
                this.config.lockExecutor.executeWithLock(task, lockConfiguration);
            }
        };
    }
}
