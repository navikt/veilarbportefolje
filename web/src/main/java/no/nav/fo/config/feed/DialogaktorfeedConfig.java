package no.nav.fo.config.feed;

import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.consumer.DialogDataFeedHandler;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.service.AktoerService;
import no.nav.sbl.dialogarena.types.Pingable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.util.Arrays.asList;

@Configuration
public class DialogaktorfeedConfig {
    @Value("${dialogaktor.feed.isalive.url}")
    private String isaliveUrl;

    @Value("${dialogaktor.feed.producer.url}")
    private String host;

    @Value("${dialogaktor.feed.consumer.pollingrate.cron}")
    private String polling;

    @Bean
    public FeedConsumer<DialogDataFraFeed> dialogDataFraFeedFeedConsumer(JdbcTemplate db, DialogDataFeedHandler callback) {
        FeedConsumerConfig<DialogDataFraFeed> config = new FeedConsumerConfig<>(
                DialogDataFraFeed.class,
                Utils.apply(DialogaktorfeedConfig::sisteEndring, db),
                host,
                "dialogaktor"
        )
                .pollingInterval(polling)
                .callback(callback)
                .interceptors(asList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }

    private static String sisteEndring(JdbcTemplate db) {
        Timestamp sisteEndring = (Timestamp) db.queryForList("SELECT dialogaktor_sist_oppdatert from METADATA").get(0).get("dialogaktor_sist_oppdatert");
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }

    @Bean
    public DialogDataFeedHandler dialogDataFeedHandler(PersistentOppdatering persistentOppdatering, JdbcTemplate db, AktoerService aktoerService) {
        return new DialogDataFeedHandler(persistentOppdatering, db, aktoerService);
    }

    @Bean
    public Pingable dialogaktorfeedPingable() {
        return Utils.urlPing("dialogaktorfeed", isaliveUrl);
    }
}
