package no.nav.fo.config.feed;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedAuthorizationModule;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.config.ApplicationConfig;
import no.nav.fo.config.unleash.UnleashService;
import no.nav.fo.config.unleash.UnleashServiceConfig;
import no.nav.fo.consumer.DedupeFeedHandler;
import no.nav.fo.consumer.OppfolgingFeedHandler;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.OppfolgingFeedRepository;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.SolrService;
import no.nav.fo.service.VeilederService;
import no.nav.sbl.jdbc.Transactor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.util.Collections.singletonList;
import static no.nav.fo.config.FeedConfig.*;
import static no.nav.fo.feed.consumer.FeedConsumerConfig.*;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;


@Configuration
@Slf4j
public class OppfolgingerfeedConfig {

    static final String SELECT_OPPFOLGING_SIST_OPPDATERT_FROM_METADATA = "SELECT oppfolging_sist_oppdatert FROM METADATA";
    static final String SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA = "SELECT oppfolging_sist_oppdatert_id FROM METADATA";

    public static final String VEILARBOPPFOLGING_URL_PROPERTY = "veilarboppfolging.api.url";

    @Inject
    private DataSource dataSource;

    @Inject
    private UnleashService unleashService;     

    private static final String UNLEASH_URL = "unleash.url";

    @Bean
    public UnleashService unleashService() {
        return new UnleashService(UnleashServiceConfig.builder()
                .applicationName(ApplicationConfig.APPLICATION_NAME)
                .unleashApiUrl(getRequiredProperty(UNLEASH_URL))
                .build());
    }

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
                () -> sisteId(db, unleashService),
                getRequiredProperty(VEILARBOPPFOLGING_URL_PROPERTY),
                BrukerOppdatertInformasjon.FEED_NAME
        );

        SimpleWebhookPollingConfig webhookPollingConfig = new SimpleWebhookPollingConfig(10, FEED_API_ROOT);

        FeedConsumerConfig<BrukerOppdatertInformasjon> config = new FeedConsumerConfig<>(baseConfig, new SimplePollingConfig(FEED_POLLING_INTERVAL_IN_SECONDS), webhookPollingConfig)
                .callback(DedupeFeedHandler.of(FEED_PAGE_SIZE, callback))
                .pageSize(FEED_PAGE_SIZE)
                .lockProvider(lockProvider(dataSource), 10000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()))
                .authorizatioModule(new OidcFeedAuthorizationModule());
        return new FeedConsumer<>(config);
    }

    @Bean
    public FeedCallback<BrukerOppdatertInformasjon> oppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                                                          BrukerRepository brukerRepository,
                                                                          SolrService solrService,
                                                                          OppfolgingFeedRepository oppfolgingFeedRepository,
                                                                          VeilederService veilederService,
                                                                          Transactor transactor) {
        return new OppfolgingFeedHandler(arbeidslisteService,
                brukerRepository,
                solrService,
                oppfolgingFeedRepository,
                veilederService,
                transactor);
    }

    private static String sisteId(JdbcTemplate db, UnleashService unleashService) {
        String id = unleashService.isEnabled("veilarbportefolje.numerisk.id.for.oppfolging") ? finnSisteIdNumerisk(db) : finnSisteIdTidspunkt(db);
        log.info("OppfolgingerfeedDebug sisteEndring: {}", id);
        return id;
    }

    static String finnSisteIdTidspunkt(JdbcTemplate db) {
        Timestamp sisteEndring = (Timestamp) db.queryForList(SELECT_OPPFOLGING_SIST_OPPDATERT_FROM_METADATA).get(0).get("oppfolging_sist_oppdatert");
        return ZonedDateTime.ofInstant(sisteEndring.toInstant().minusSeconds(10), ZoneId.systemDefault()).toString();
    }

    static String finnSisteIdNumerisk(JdbcTemplate db) {
        return ((BigDecimal) db.queryForList(SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA).get(0).get("oppfolging_sist_oppdatert_id")).toPlainString();
    }
}
