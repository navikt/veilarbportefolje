package no.nav.fo.config.feed;

import no.nav.brukerdialog.security.oidc.OidcFeedAuthorizationModule;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.consumer.OppfolgingFeedHandler;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.OppfolgingFeedRepository;
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
public class OppfolgingerfeedConfig {

    @Value("${veilarboppfolging.api.url}")
    private String host;

    @Value("${oppfolging.feed.consumer.pollingrate.cron}")
    private String polling;

    @Value("${oppfolging.feed.consumer.pollingratewebhook.cron}")
    private String webhookPolling;

    @Value("${oppfolging.feed.pagesize:500}")
    private int pageSize;

    @Bean
    public FeedConsumer<BrukerOppdatertInformasjon> brukerOppdatertInformasjonFeedConsumer(JdbcTemplate db, OppfolgingFeedHandler callback) {
        BaseConfig<BrukerOppdatertInformasjon> baseConfig = new BaseConfig<>(
                BrukerOppdatertInformasjon.class,
                Utils.apply(OppfolgingerfeedConfig::sisteEndring, db),
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
    public OppfolgingFeedHandler oppfolgingFeedHandler(OppdaterBrukerdataFletter oppdaterBrukerdataFletter,
                                                       ArbeidslisteService arbeidslisteService,
                                                       BrukerRepository brukerRepository,
                                                       AktoerService aktoerService,
                                                       SolrService solrService,
                                                       OppfolgingFeedRepository oppfolgingFeedRepository) {
        return new OppfolgingFeedHandler(oppdaterBrukerdataFletter, arbeidslisteService, brukerRepository, aktoerService, solrService, oppfolgingFeedRepository);
    }

    private static String sisteEndring(JdbcTemplate db) {
        Timestamp sisteEndring = (Timestamp) db.queryForList("SELECT oppfolging_sist_oppdatert from METADATA").get(0).get("oppfolging_sist_oppdatert");
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }
}
