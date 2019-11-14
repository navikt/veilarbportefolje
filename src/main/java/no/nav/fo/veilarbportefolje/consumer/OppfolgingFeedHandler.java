package no.nav.fo.veilarbportefolje.consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.database.OppfolgingFeedRepository;
import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.fo.veilarbportefolje.domene.VeilederId;
import no.nav.fo.veilarbportefolje.indeksering.ElasticIndexer;
import no.nav.fo.veilarbportefolje.service.ArbeidslisteService;
import no.nav.fo.veilarbportefolje.service.VeilederService;
import no.nav.sbl.jdbc.Transactor;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Comparator.naturalOrder;
import static no.nav.metrics.MetricsFactory.getMeterRegistry;

@Slf4j
public class OppfolgingFeedHandler implements FeedCallback<BrukerOppdatertInformasjon> {


    private static final String FEED_NAME = "oppfolging";
    private final Counter antallTotaltMetrikk;
    private static long lastEntry;

    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;
    private ElasticIndexer elasticIndexer;
    private OppfolgingFeedRepository oppfolgingFeedRepository;
    private VeilederService veilederService;
    private Transactor transactor;

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

        antallTotaltMetrikk = Counter.builder("portefolje_feed").tag("feed_name", FEED_NAME).register(getMeterRegistry());
        Gauge.builder("portefolje_feed_last_id", OppfolgingFeedHandler::getLastEntry).tag("feed_name", FEED_NAME).register(getMeterRegistry());

    }

    private static long getLastEntry() {
        return lastEntry;
    }

    @Override
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {
        try {
            log.info("OppfolgingerfeedDebug data: {}", data);

            data.forEach(info -> {
                if (info.getStartDato() == null) {
                    log.warn("Bruker {} har ingen startdato", info.getAktoerid());
                }
                oppdaterOppfolgingData(info);
                elasticIndexer.indekserAsynkront(AktoerId.of(info.getAktoerid()));
                antallTotaltMetrikk.increment();
            });

            finnMaxFeedId(data).ifPresent(id -> {
                oppfolgingFeedRepository.updateOppfolgingFeedId(id);
                lastEntry = id.longValue();
            });

        } catch (Exception e) {
            String message = "Feil ved behandling av oppfølgingsdata (oppfolging) fra feed for liste med brukere.";
            log.error(message, e);
        }
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
