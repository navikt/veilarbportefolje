package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.database.Transactor;
import no.nav.pto.veilarbportefolje.domene.value.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedCallback;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Comparator.naturalOrder;
import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.KAFKA_OPPFOLGING;
import static no.nav.pto.veilarbportefolje.elastic.MetricsReporter.getMeterRegistry;

@Slf4j
public class OppfolgingFeedHandler implements FeedCallback {
    private static final String FEED_NAME = "oppfolging";

    private final ArbeidslisteService arbeidslisteService;
    private final BrukerService brukerService;
    private final ElasticIndexer elasticIndexer;
    private final OppfolgingRepository oppfolgingRepository;
    private final Transactor transactor;
    private final LeaderElectionClient leaderElectionClient;
    private final UnleashService unleashService;

    private static BigDecimal lastEntry;

    public OppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                 BrukerService brukerService,
                                 ElasticIndexer elasticIndexer,
                                 OppfolgingRepository oppfolgingRepository,
                                 Transactor transactor,
                                 LeaderElectionClient leaderElectionClient, UnleashService unleashService) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerService = brukerService;
        this.elasticIndexer = elasticIndexer;
        this.oppfolgingRepository = oppfolgingRepository;
        this.transactor = transactor;
        this.leaderElectionClient = leaderElectionClient;
        this.unleashService = unleashService;

        Gauge.builder("portefolje_feed_last_id", OppfolgingFeedHandler::getLastEntry).tag("feed_name", FEED_NAME).register(getMeterRegistry());
    }

    private static BigDecimal getLastEntry() {
        return lastEntry;
    }

    @Override
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {
        if (!leaderElectionClient.isLeader()) {
            return;
        }

        if (unleashService.isEnabled(KAFKA_OPPFOLGING)) {
            return;
        }

        MDC.put(PREFERRED_NAV_CALL_ID_HEADER_NAME, generateId());
        log.info("OppfolgingerfeedDebug data: {}", data);

        try {
            data.forEach(info -> {
                if (info.getStartDato() == null) {
                    log.warn("Bruker {} har ingen startdato", info.getAktoerid());
                }
                oppdaterOppfolgingData(info);

                elasticIndexer.indekser(AktoerId.of(info.getAktoerid()));
            });

            finnMaxFeedId(data).ifPresent(id -> {
                oppfolgingRepository.updateOppfolgingFeedId(id);
                lastEntry = id;
            });
        } catch (Exception e) {
            String message = "Feil ved behandling av oppfølgingsdata (oppfolging) fra feed for liste med brukere.";
            log.error(message, e);
        } finally {
            MDC.remove(PREFERRED_NAV_CALL_ID_HEADER_NAME);
        }
    }

    static Optional<BigDecimal> finnMaxFeedId(List<BrukerOppdatertInformasjon> data) {
        return data.stream().map(BrukerOppdatertInformasjon::getFeedId).filter(Objects::nonNull).max(naturalOrder());
    }

    private void oppdaterOppfolgingData(BrukerOppdatertInformasjon oppfolgingData) {
        AktoerId aktoerId = AktoerId.of(oppfolgingData.getAktoerid());

        final boolean byttetNavKontor = arbeidslisteService.brukerHarByttetNavKontor(aktoerId);
        boolean skalSletteArbeidsliste = brukerErIkkeUnderOppfolging(oppfolgingData) || byttetNavKontor;
        transactor.inTransaction(() -> {
            if (skalSletteArbeidsliste) {

                log.info("Sletter arbeidsliste for bruker {} med oppfølging={} og byttetNavkontor={}",
                        oppfolgingData.getAktoerid(),
                        oppfolgingData.getOppfolging(),
                        byttetNavKontor);

                arbeidslisteService.deleteArbeidslisteForAktoerId(aktoerId);
            }
            oppfolgingRepository.oppdaterOppfolgingData(oppfolgingData);
        });

    }

    public static boolean brukerErIkkeUnderOppfolging(BrukerOppdatertInformasjon oppfolgingData) {
        return !oppfolgingData.getOppfolging();
    }
}
