package no.nav.fo.config.feed;

import no.nav.brukerdialog.security.oidc.OidcFeedAuthorizationModule;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.consumer.SituasjonFeedHandler;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.fo.service.SolrService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.util.Collections.singletonList;
import static no.nav.fo.config.FeedConfig.FEED_API_ROOT;
import static no.nav.fo.feed.consumer.FeedConsumerConfig.*;


@Configuration
public class SituasjonerfeedConfig {

    @Value("${veilarbsituasjon.api.url}")
    private String host;

    @Value("${situasjon.feed.consumer.pollingrate.cron}")
    private String polling;

    @Value("${situasjon.feed.consumer.pollingratewebhook.cron}")
    private String webhookPolling;

    @Value("${situasjon.feed.pagesize ?: 500}")
    private int pageSize;

    @Bean
    public FeedConsumer<BrukerOppdatertInformasjon> brukerOppdatertInformasjonFeedConsumer(JdbcTemplate db, SituasjonFeedHandler callback) {
        BaseConfig<BrukerOppdatertInformasjon> baseConfig = new BaseConfig<>(
                BrukerOppdatertInformasjon.class,
                Utils.apply(SituasjonerfeedConfig::sisteEndring, db),
                host,
                BrukerOppdatertInformasjon.FEED_NAME
        );

        WebhookPollingConfig webhookPollingConfig = new WebhookPollingConfig(webhookPolling,FEED_API_ROOT);

        FeedConsumerConfig<BrukerOppdatertInformasjon> config = new FeedConsumerConfig<>(baseConfig, new PollingConfig(polling), webhookPollingConfig)
                .callback(callback)
                .pageSize(pageSize)
                .interceptors(singletonList(new OidcFeedOutInterceptor()))
                .authorizatioModule(new OidcFeedAuthorizationModule());
        return new FeedConsumer<>(config);
    }

    @Bean
    public SituasjonFeedHandler situasjonFeedHandler(OppdaterBrukerdataFletter oppdaterBrukerdataFletter,
                                                      ArbeidslisteService arbeidslisteService,
                                                      BrukerRepository brukerRepository,
                                                      AktoerService aktoerService,
                                                      SolrService solrService) {
        return new SituasjonFeedHandler(oppdaterBrukerdataFletter, arbeidslisteService, brukerRepository, aktoerService, solrService);
    }

    private static String sisteEndring(JdbcTemplate db) {
        Timestamp sisteEndring = (Timestamp) db.queryForList("SELECT situasjon_sist_oppdatert from METADATA").get(0).get("situasjon_sist_oppdatert");
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }
}
