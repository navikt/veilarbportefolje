package no.nav.pto.veilarbportefolje.feed.consumer;

import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.feed.common.FeedAuthorizationModule;
import okhttp3.OkHttpClient;
import org.quartz.ScheduleBuilder;

import java.util.function.Supplier;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

public class FeedConsumerConfig {

    public final Class<BrukerOppdatertInformasjon> domainobject;
    public final Supplier<String> lastEntrySupplier;
    public final String host;
    public final String feedName;

    public final ScheduleCreator pollingConfig;
    public final WebhookScheduleCreator webhookPollingConfig;
    public OkHttpClient client;

    FeedCallback callback;
    FeedAuthorizationModule authorizationModule = (feedname) -> true;
    int pageSize;

    public FeedConsumerConfig(BaseConfig<BrukerOppdatertInformasjon> baseConfig, ScheduleCreator pollingConfig) {
        this(baseConfig, pollingConfig, null);
    }

    public FeedConsumerConfig(BaseConfig<BrukerOppdatertInformasjon> baseConfig, ScheduleCreator pollingConfig, WebhookScheduleCreator webhookPollingConfig) {
        this.domainobject = baseConfig.domainobject;
        this.lastEntrySupplier = baseConfig.lastEntrySupplier;
        this.host = baseConfig.host;
        this.feedName = baseConfig.feedName;
        this.pollingConfig = pollingConfig;
        this.webhookPollingConfig = webhookPollingConfig;

        this.pageSize = 100;
    }

    public FeedConsumerConfig authorizatioModule(FeedAuthorizationModule authorizationModule) {
        this.authorizationModule = authorizationModule;
        return this;
    }

    public FeedConsumerConfig callback(FeedCallback callback) {
        this.callback = callback;
        return this;
    }

    public FeedConsumerConfig restClient(OkHttpClient client) {
        this.client = client;
        return this;
    }


    public FeedConsumerConfig pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public static class BaseConfig<DOMAINOBJECT> {
        public final Class<DOMAINOBJECT> domainobject;
        public final Supplier<String> lastEntrySupplier;
        public final String host;
        public final String feedName;

        public BaseConfig(Class<DOMAINOBJECT> domainobject, Supplier<String> lastEntrySupplier, String host, String feedName) {
            this.domainobject = domainobject;
            this.lastEntrySupplier = lastEntrySupplier;
            this.host = host;
            this.feedName = feedName;
        }
    }

    public static class ScheduleCreator {
        public final ScheduleBuilder<?> scheduleBuilder;

        public ScheduleCreator(ScheduleBuilder<?> scheduleBuilder) {
            this.scheduleBuilder = scheduleBuilder;
        }
    }
    public static class WebhookScheduleCreator extends ScheduleCreator {
        public final String apiRootPath;

        public WebhookScheduleCreator(ScheduleBuilder<?> builder, String apiRootPath) {
            super(builder);
            this.apiRootPath = apiRootPath;
        }
    }

    public static class CronPollingConfig extends ScheduleCreator {
        public CronPollingConfig(String pollingInterval) {
            super(cronSchedule(pollingInterval));
        }
    }

    public static class SimplePollingConfig extends ScheduleCreator {
        public SimplePollingConfig(int pollingIntervalInSeconds) {
            super(simpleSchedule().withIntervalInSeconds(pollingIntervalInSeconds).repeatForever());
        }
    }

    public static class CronWebhookPollingConfig extends WebhookScheduleCreator {
        public CronWebhookPollingConfig(String webhookPollingInterval, String apiRootPath) {
            super(cronSchedule(webhookPollingInterval), apiRootPath);
        }
    }

    public static class SimpleWebhookPollingConfig extends WebhookScheduleCreator {
        public SimpleWebhookPollingConfig(int webhookPollingIntervalInSeconds, String apiRootPath) {
            super(simpleSchedule().withIntervalInSeconds(webhookPollingIntervalInSeconds).repeatForever(), apiRootPath);
        }
    }
}
