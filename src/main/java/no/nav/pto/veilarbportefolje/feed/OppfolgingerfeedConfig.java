package no.nav.pto.veilarbportefolje.feed;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.brukerdialog.security.oidc.OidcFeedAuthorizationModule;
import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.pto.veilarbportefolje.feed.oppfolging.OppfolgingFeedHandler;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.feed.oppfolging.OppfolgingFeedRepository;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.service.VeilederService;
import no.nav.sbl.jdbc.Transactor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.math.BigDecimal;

import static java.math.BigDecimal.valueOf;
import static java.util.Collections.singletonList;
import static no.nav.fo.feed.consumer.FeedConsumerConfig.*;
import static no.nav.pto.veilarbportefolje.config.ApplicationConfig.VEILARBOPPFOLGING_URL_PROPERTY;
import static no.nav.pto.veilarbportefolje.feed.FeedConfig.*;
import static no.nav.sbl.util.EnvironmentUtils.getRequiredProperty;


@Configuration
public class OppfolgingerfeedConfig {

    public static final String SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA = "SELECT oppfolging_sist_oppdatert_id FROM METADATA";

    @Inject
    private DataSource dataSource;

    private LockProvider lockProvider(DataSource dataSource) {
        return new JdbcLockProvider(dataSource);
    }

    @Bean
    public FeedConsumer<BrukerOppdatertInformasjon> brukerOppdatertInformasjonFeedConsumer(
            JdbcTemplate db,
            FeedCallback<BrukerOppdatertInformasjon> callback) {
        BaseConfig<BrukerOppdatertInformasjon> baseConfig = new BaseConfig<>(
                BrukerOppdatertInformasjon.class,
                () -> nesteId(db),
                getRequiredProperty(VEILARBOPPFOLGING_URL_PROPERTY),
                BrukerOppdatertInformasjon.FEED_NAME
        );

        SimpleWebhookPollingConfig webhookPollingConfig = new SimpleWebhookPollingConfig(10, FEED_API_ROOT);

        FeedConsumerConfig<BrukerOppdatertInformasjon> config = new FeedConsumerConfig<>(baseConfig, new SimplePollingConfig(FEED_POLLING_INTERVAL_IN_SECONDS), webhookPollingConfig)
                .callback(callback)
                .pageSize(FEED_PAGE_SIZE)
                .lockProvider(lockProvider(dataSource), 10000)
                .interceptors(singletonList(new OidcFeedOutInterceptor()))
                .authorizatioModule(new OidcFeedAuthorizationModule());
        return new FeedConsumer<>(config);
    }

    @Bean
    public FeedCallback<BrukerOppdatertInformasjon> oppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                                                          BrukerRepository brukerRepository,
                                                                          ElasticIndexer elasticIndexer,
                                                                          OppfolgingFeedRepository oppfolgingFeedRepository,
                                                                          VeilederService veilederService,
                                                                          Transactor transactor) {
        return new OppfolgingFeedHandler(arbeidslisteService,
                brukerRepository,
                elasticIndexer,
                oppfolgingFeedRepository,
                veilederService,
                transactor);
    }

    public static String nesteId(JdbcTemplate db) {
        return ((BigDecimal) db.queryForList(SELECT_OPPFOLGING_SIST_OPPDATERT_ID_FROM_METADATA).stream()
                .findFirst()
                .map(m -> m.get("oppfolging_sist_oppdatert_id"))
                .orElse(valueOf(0)))
                .add(valueOf(1))
                .toPlainString();
    }
}
