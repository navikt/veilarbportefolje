package no.nav.fo.config;

import no.nav.brukerdialog.security.oidc.OidcFeedOutInterceptor;
import no.nav.fo.consumer.AktivitetFeedHandler;
import no.nav.fo.consumer.DialogDataFeedHandler;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.feed.consumer.FeedConsumer;
import no.nav.fo.feed.consumer.FeedConsumerConfig;
import no.nav.fo.feed.controller.FeedController;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.fo.util.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import java.util.Collections;

import no.nav.fo.consumer.TilordningFeedHandler;

@Configuration
public class FeedConfig {
    static {
        FeedConsumer.applicationApiroot = "veilarbportefolje/tjenester";
    }

    @Value("${dialogaktor.feed.isalive.url}")
    private String dialogIsalive;

    @Value("${dialogaktor.feed.producer.url}")
    private String dialogaktorHost;

    @Value("${dialogaktor.feed.consumer.pollingrate.cron}")
    private String dialogaktorPolling;

    // F.eks http://localhost:8486/veilarbsituasjon/api
    @Value("${tilordninger.feed.producer.url}")
    private String tilordningerHost;

    @Value("${tilordninger.feed.consumer.pollingrate.cron}")
    private String pollingRate;

    @Value("${tilordninger.feed.consumer.pollingratewebhook.cron}")
    private String pollingRateWebhook;

    @Value("${aktiviteter.feed.producer.url}")
    private String aktiviteterHost;

    @Value("${aktiviteter.feed.consumer.pollingrate.cron}")
    private String aktiviteterPolling;



    @Bean
    public FeedController feedController(JdbcTemplate db, BrukerRepository brukerRepository, DialogDataFeedHandler callback,  AktivitetFeedHandler aktiviteterCallback) {
        FeedController feedController = new FeedController();

//        feedController.addFeed("tilordninger", oppfolgingBrukerFeed());
        feedController.addFeed("aktiviteter", aktiviteterFraFeedConsumer(brukerRepository,aktiviteterCallback));
        feedController.addFeed("dialogaktor", dialogDataFraFeedFeedConsumer(db, callback));

        return feedController;
    }

    private FeedConsumer<BrukerOppdatertInformasjon> oppfolgingBrukerFeed() {
        FeedConsumerConfig<BrukerOppdatertInformasjon> config = new FeedConsumerConfig<>(
                BrukerOppdatertInformasjon.class,
                () -> "1970-01-01T00:00:00.000+02:00",
                tilordningerHost,
                "tilordninger"
        );

        config.pollingInterval(pollingRate);
        config.webhookPollingInterval(pollingRateWebhook);
        config.callback((last, page) -> tilordningFeedHandler().handleFeedPage((page)));
        config.interceptors(asList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }

    private TilordningFeedHandler tilordningFeedHandler() {
        return new TilordningFeedHandler(oppdaterBrukerdataFletter());
    }


    @Bean
    public OppdaterBrukerdataFletter oppdaterBrukerdataFletter() {
        return new OppdaterBrukerdataFletter();
    }

    private FeedConsumer<DialogDataFraFeed> dialogDataFraFeedFeedConsumer(JdbcTemplate db, DialogDataFeedHandler callback) {
        Supplier<String> lastEntrySupplier = () -> {
            Timestamp sisteEndring = (Timestamp) db.queryForList("SELECT dialogaktor_sist_oppdatert from METADATA").get(0).get("dialogaktor_sist_oppdatert");
            return ZonedDateTime.ofInstant(sisteEndring.toInstant(), ZoneId.systemDefault()).toString();
        };

        FeedConsumerConfig<DialogDataFraFeed> config = new FeedConsumerConfig<>(
                DialogDataFraFeed.class,
                lastEntrySupplier,
                dialogaktorHost,
                "dialogaktor"
        )
                .pollingInterval(dialogaktorPolling)
                .callback(callback)
                .interceptors(asList(new OidcFeedOutInterceptor()));

        return new FeedConsumer<>(config);
    }

    @Bean
    public DialogDataFeedHandler dialogDataFeedHandler() {
        return new DialogDataFeedHandler();
    }

    @Bean
    public Pingable veilarbDialogPing() {
        final String name = "dialogaktorfeed";
        return new Pingable() {
            @Override
            public Ping ping() {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(dialogIsalive).openConnection();
                    connection.connect();
                    if (connection.getResponseCode() == 200) {
                        return Ping.lyktes(name);
                    }
                } catch (IOException e) {
                    return Ping.feilet(name, e);
                }
                return Ping.feilet(name, new RuntimeException("Noe gikk feil."));
            }
        };
    }

    private FeedConsumer<AktivitetDataFraFeed> aktiviteterFraFeedConsumer(BrukerRepository brukerRepository, AktivitetFeedHandler aktiviteterCallback) {
        Supplier<String> lastEntrySupplier = () -> {
            Timestamp sisteEndring = (Timestamp) brukerRepository.getAktiviteterSistOppdatert();
            return DateUtils.ISO8601FromTimestamp(sisteEndring, ZoneId.systemDefault());
        };;

        FeedConsumerConfig<AktivitetDataFraFeed> config = new FeedConsumerConfig<>(
                AktivitetDataFraFeed.class,
                lastEntrySupplier,
                aktiviteterHost,
                "aktiviteter"
        );

        config.interceptors(Collections.singletonList(new OidcFeedOutInterceptor()));
        config.pollingInterval(aktiviteterPolling);
        config.callback(aktiviteterCallback);

        return new FeedConsumer<>(config);
    }

    @Bean
    public AktivitetFeedHandler aktiviteterFeedHandler() {
        return new AktivitetFeedHandler();
    }
}
