package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.database.Transactor;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
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

@Slf4j
public class OppfolgingFeedHandler implements FeedCallback {
    private static final String FEED_NAME = "oppfolging";

    private final ArbeidslisteService arbeidslisteService;
    private final BrukerService brukerService;
    private final ElasticIndexer elasticIndexer;
    private final OppfolgingRepository oppfolgingRepository;
    private final Transactor transactor;
    private final LeaderElectionClient leaderElectionClient;

    private static BigDecimal lastEntry;

    public OppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                 BrukerService brukerService,
                                 ElasticIndexer elasticIndexer,
                                 OppfolgingRepository oppfolgingRepository,
                                 Transactor transactor,
                                 LeaderElectionClient leaderElectionClient) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerService = brukerService;
        this.elasticIndexer = elasticIndexer;
        this.oppfolgingRepository = oppfolgingRepository;
        this.transactor = transactor;
        this.leaderElectionClient = leaderElectionClient;
    }

    private static BigDecimal getLastEntry() {
        return lastEntry;
    }

    @Override
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {
        if (!leaderElectionClient.isLeader()) {
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

        final boolean byttetNavKontor = brukerHarByttetNavKontor(aktoerId);
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

    boolean brukerHarByttetNavKontor(AktoerId aktoerId) {
        Optional<String> navKontorForArbeidsliste =
                arbeidslisteService.hentNavKontorForArbeidsliste(aktoerId);

        if (navKontorForArbeidsliste.isEmpty()) {
            log.info("Bruker {} har ikke NAV-kontor på arbeidsliste", aktoerId.toString());
            return false;
        }

        final Optional<String> navKontorForBruker = brukerService.hentNavKontor(aktoerId);
        if (navKontorForBruker.isEmpty()) {
            log.error("Kunne ikke hente NAV-kontor fra db-link til arena for bruker {}", aktoerId.toString());
            return false;
        }

        log.info("Bruker {} er på kontor {} mens arbeidslisten er lagret på {}", aktoerId.toString(), navKontorForBruker.get(), navKontorForArbeidsliste.get());
        return !navKontorForBruker.orElseThrow().equals(navKontorForArbeidsliste.orElseThrow());
    }

    public static boolean brukerErIkkeUnderOppfolging(BrukerOppdatertInformasjon oppfolgingData) {
        return !oppfolgingData.getOppfolging();
    }
}
