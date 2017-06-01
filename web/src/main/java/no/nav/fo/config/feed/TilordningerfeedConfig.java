package no.nav.fo.config.feed;

import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.consumer.TilordningFeedHandler;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.service.OppdaterBrukerdataFletter;
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
public class TilordningerfeedConfig {
    @Value("${tilordninger.feed.isalive.url}")
    private String isaliveUrl;

    @Value("${tilordninger.feed.producer.url}")
    private String host;

    @Value("${tilordninger.feed.consumer.pollingrate.cron}")
    private String polling;

    @Value("${tilordninger.feed.consumer.pollingratewebhook.cron}")
    private String webhookPolling;

    @Bean
    public FeedConsumer<BrukerOppdatertInformasjon> brukerOppdatertInformasjonFeedConsumer(JdbcTemplate db, TilordningFeedHandler callback) {
        FeedConsumerConfig<BrukerOppdatertInformasjon> config = new FeedConsumerConfig<>(
                BrukerOppdatertInformasjon.class,
                Utils.apply(TilordningerfeedConfig::sisteEndring, db),
                host,
                "tilordninger"
        )
                .pollingInterval(polling)
                .webhookPollingInterval(webhookPolling)
                .callback(callback)
                .interceptors(asList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }

    @Bean
    public TilordningFeedHandler tilordningFeedHandler(OppdaterBrukerdataFletter oppdaterBrukerdataFletter) {
        return new TilordningFeedHandler(oppdaterBrukerdataFletter);
    }

    @Bean
    public Pingable tilordningerfeedPingable() {
        return Utils.urlPing("tilordningerfeed", isaliveUrl);
    }

    private static String sisteEndring(JdbcTemplate db) {
        Timestamp sisteEndring = (Timestamp) db.queryForList("SELECT dialogaktor_sist_oppdatert from METADATA").get(0).get("dialogaktor_sist_oppdatert");
        return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
    }
}
