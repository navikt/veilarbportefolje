package no.nav.pto.veilarbportefolje.feed.oppfolging;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import no.nav.metrics.utils.MetricsUtils;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.VeilederService;
import no.nav.sbl.jdbc.Transactor;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Comparator.naturalOrder;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;

@Slf4j
public class OppfolgingFeedHandler implements FeedCallback<BrukerOppdatertInformasjon> {


    private static final String FEED_NAME = "oppfolging";
    private static BigDecimal lastEntry;
    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;
    private ElasticIndexer elasticIndexer;
    private OppfolgingFeedRepository oppfolgingFeedRepository;
    private VeilederService veilederService;
    private Transactor transactor;
    private final Counter antallTotaltMetrikk;

    @Inject
    public OppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                 BrukerRepository brukerRepository,
                                 ElasticIndexer elasticIndexer,
                                 OppfolgingFeedRepository oppfolgingFeedRepository,
                                 VeilederService veilederService,
                                 Transactor transactor) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerRepository = brukerRepository;
        this.elasticIndexer = elasticIndexer;
        this.oppfolgingFeedRepository = oppfolgingFeedRepository;
        this.veilederService = veilederService;
        this.transactor = transactor;

        Gauge.builder("portefolje_feed_last_id", OppfolgingFeedHandler::getLastEntry).tag("feed_name", FEED_NAME).register(getMeterRegistry());
        antallTotaltMetrikk = Counter.builder("portefolje_feed").tag("feed_name", FEED_NAME).register(getMeterRegistry());

    }

    private static BigDecimal getLastEntry() {
        return lastEntry;
    }

    @Override
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {

        log.info("OppfolgingerfeedDebug data: {}", data);

        MetricsUtils.timed("feed.oppfolging", () -> {
            try {

                data.forEach(info -> {
                    if (info.getStartDato() == null) {
                        log.warn("Bruker {} har ingen startdato", info.getAktoerid());
                    }
                    oppdaterOppfolgingData(info);

                    elasticIndexer.indekserAsynkront(AktoerId.of(info.getAktoerid()));


                });

                finnMaxFeedId(data).ifPresent(id -> {
                    oppfolgingFeedRepository.updateOppfolgingFeedId(id);
                    lastEntry = id;
                });

                antallTotaltMetrikk.increment(data.size());

            } catch (Exception e) {
                String message = "Feil ved behandling av oppfølgingsdata (oppfolging) fra feed for liste med brukere.";
                log.error(message, e);
            }
        });
    }

    static Optional<BigDecimal> finnMaxFeedId(List<BrukerOppdatertInformasjon> data) {
        return data.stream().map(BrukerOppdatertInformasjon::getFeedId).filter(Objects::nonNull).max(naturalOrder());
    }

    private void oppdaterOppfolgingData(BrukerOppdatertInformasjon oppfolgingData) {
        AktoerId aktoerId = AktoerId.of(oppfolgingData.getAktoerid());

        boolean skalSletteArbeidsliste = brukerErIkkeUnderOppfolging(oppfolgingData) || eksisterendeVeilederHarIkkeTilgangTilBrukerSinEnhet(aktoerId);

        transactor.inTransaction(() -> {
            if (skalSletteArbeidsliste) {
                arbeidslisteService.deleteArbeidslisteForAktoerid(aktoerId);
            }
            oppfolgingFeedRepository.oppdaterOppfolgingData(oppfolgingData);
        });

    }

    private static boolean brukerErIkkeUnderOppfolging(BrukerOppdatertInformasjon oppfolgingData) {
        return !oppfolgingData.getOppfolging();
    }

    private boolean eksisterendeVeilederHarIkkeTilgangTilBrukerSinEnhet(AktoerId aktoerId) {
        return !oppfolgingFeedRepository
                .retrieveOppfolgingData(aktoerId.toString())
                .map(oppfolgingData -> veilederHarTilgangTilEnhet(aktoerId, oppfolgingData))
                .getOrElse(false);
    }

    private boolean veilederHarTilgangTilEnhet(AktoerId aktoerId, BrukerOppdatertInformasjon oppfolgingData) {
        VeilederId veilederId = VeilederId.of(oppfolgingData.getVeileder());
        return brukerRepository
                .retrievePersonid(aktoerId)
                .flatMap(brukerRepository::retrieveEnhet)
                .map(enhet -> veilederService.getIdenter(enhet).contains(veilederId))
                .getOrElse(false);
    }
}
