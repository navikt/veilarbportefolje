package no.nav.fo.config.feed;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedAuthorizationModule;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.consumer.NyOppfolgingFeedHandler;
import no.nav.fo.consumer.OppfolgingFeedHandler;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.OppfolgingFeedRepository;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.SolrService;
import no.nav.sbl.jdbc.Transactor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.util.Collections.singletonList;
import static no.nav.fo.config.FeedConfig.FEED_API_ROOT;
import static no.nav.fo.feed.consumer.FeedConsumerConfig.*;


@Configuration
@Slf4j
public class OppfolgingerfeedConfig {

    @Value("${veilarboppfolging.api.url}")
    private String host;

    @Value("${oppfolging.feed.pagesize:500}")
    private int pageSize;
    
    @Value("${ny.oppfolgingfeedhandler:true}")
    private boolean nyHandler;

    @Value("${oppfolging.feed.pollingintervalseconds: 10}")
    private int pollingIntervalInSeconds;

    @Inject
    private DataSource dataSource;

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    @Bean
    public FeedConsumer<BrukerOppdatertInformasjon> brukerOppdatertInformasjonFeedConsumer(
            JdbcTemplate db, 
            FeedCallback<BrukerOppdatertInformasjon> callback) {
        BaseConfig<BrukerOppdatertInformasjon> baseConfig = new BaseConfig<>(
                BrukerOppdatertInformasjon.class,
                Utils.apply(OppfolgingerfeedConfig::sisteEndring, db),
                host,
                BrukerOppdatertInformasjon.FEED_NAME
        );

        SimpleWebhookPollingConfig webhookPollingConfig = new SimpleWebhookPollingConfig(10, FEED_API_ROOT);

        FeedConsumerConfig<BrukerOppdatertInformasjon> config = new FeedConsumerConfig<>(baseConfig, new SimplePollingConfig(pollingIntervalInSeconds), webhookPollingConfig)
                .callback(callback)
                .pageSize(pageSize)
                .lockProvider(lockProvider(dataSource), 10000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()))
                .authorizatioModule(new OidcFeedAuthorizationModule());
        return new FeedConsumer<>(config);
    }

    @Bean
    public FeedCallback<BrukerOppdatertInformasjon> oppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                                       BrukerRepository brukerRepository,
                                                       AktoerService aktoerService,
                                                       SolrService solrService,
                                                       OppfolgingFeedRepository oppfolgingFeedRepository,
                                                       Transactor transactor) {
        return nyHandler 
                ? new NyOppfolgingFeedHandler(arbeidslisteService, brukerRepository, solrService, oppfolgingFeedRepository, transactor)
                : new OppfolgingFeedHandler(arbeidslisteService, brukerRepository, aktoerService, solrService, oppfolgingFeedRepository);
    }

    private static String sisteEndring(JdbcTemplate db) {
        Timestamp sisteEndring = (Timestamp) db.queryForList("SELECT oppfolging_sist_oppdatert FROM METADATA").get(0).get("oppfolging_sist_oppdatert");
        String sisteEndringStr = ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
        log.info("OppfolgingerfeedDebug sisteEndring: {}", sisteEndringStr);
        return sisteEndringStr;
    }
}
