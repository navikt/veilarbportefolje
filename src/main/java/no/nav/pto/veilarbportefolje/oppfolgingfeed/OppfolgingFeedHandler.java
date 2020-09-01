package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import io.micrometer.core.instrument.Gauge;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.leaderelection.LeaderElectionClient;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.database.Transactor;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedCallback;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Comparator.naturalOrder;
import static no.nav.common.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static no.nav.common.utils.IdUtils.generateId;
import static no.nav.pto.veilarbportefolje.elastic.MetricsReporter.getMeterRegistry;

@Slf4j
public class OppfolgingFeedHandler implements FeedCallback {


    private static final String FEED_NAME = "oppfolging";

    private static BigDecimal lastEntry;
    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;
    private ElasticIndexer elasticIndexer;
    private OppfolgingRepository oppfolgingRepository;
    private VeilarbVeilederClient veilarbVeilederClient;
    private Transactor transactor;
    private final UnleashService unleashService;
    private final LeaderElectionClient leaderElectionClient;

    public OppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                 BrukerRepository brukerRepository,
                                 ElasticIndexer elasticIndexer,
                                 OppfolgingRepository oppfolgingRepository,
                                 VeilarbVeilederClient veilarbVeilederClient,
                                 Transactor transactor,
                                 LeaderElectionClient leaderElectionClient,
                                 UnleashService unleashService) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerRepository = brukerRepository;
        this.elasticIndexer = elasticIndexer;
        this.oppfolgingRepository = oppfolgingRepository;
        this.veilarbVeilederClient = veilarbVeilederClient;
        this.transactor = transactor;
        this.unleashService = unleashService;
        this.leaderElectionClient = leaderElectionClient;

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

        Try<BrukerOppdatertInformasjon> hentOppfolgingData = oppfolgingRepository.retrieveOppfolgingData(aktoerId);

        boolean brukerErIkkeUnderOppfolging = brukerErIkkeUnderOppfolging(oppfolgingData);
        boolean eksisterendeVeilederHarIkkeTilgangTilBrukerSinEnhet = eksisterendeVeilederHarIkkeTilgangTilBrukerSinEnhet(hentOppfolgingData, aktoerId);

        boolean skalSletteArbeidsliste;
        if (unleashService.isEnabled("portefolje.endre_arbeidsliste_logikk")) {
            skalSletteArbeidsliste = brukerErIkkeUnderOppfolging || brukerHarByttetNavKontor(aktoerId);
        } else {
            skalSletteArbeidsliste = brukerErIkkeUnderOppfolging || eksisterendeVeilederHarIkkeTilgangTilBrukerSinEnhet;
        }

        transactor.inTransaction(() -> {
            if (skalSletteArbeidsliste) {
                log.info("Sletter arbeidsliste for bruker {}, under oppfolging {}, har ikke tilgang til enhet {} ", aktoerId, brukerErIkkeUnderOppfolging, eksisterendeVeilederHarIkkeTilgangTilBrukerSinEnhet);
                arbeidslisteService.deleteArbeidslisteForAktoerId(aktoerId);
            }
            oppfolgingRepository.oppdaterOppfolgingData(oppfolgingData);
        });

    }

    private boolean brukerHarByttetNavKontor(AktoerId aktoerId) {
        String navKontorArbeidslisteErLagretPaa =
                arbeidslisteService.hentNavKontorForArbeidsliste(aktoerId)
                        .orElseThrow(IllegalStateException::new);

        String navKontorBrukerErPaa =
                brukerRepository.hentNavKontor(aktoerId)
                        .orElseThrow(IllegalStateException::new);


        log.info("Bruker {} er på kontor {} mens arbeidslisten er lagret på {}", aktoerId.toString(), navKontorBrukerErPaa, navKontorArbeidslisteErLagretPaa);

        return !navKontorBrukerErPaa.equals(navKontorArbeidslisteErLagretPaa);
    }

    public static boolean brukerErIkkeUnderOppfolging(BrukerOppdatertInformasjon oppfolgingData) {
        return !oppfolgingData.getOppfolging();
    }

    public boolean eksisterendeVeilederHarIkkeTilgangTilBrukerSinEnhet(Try<BrukerOppdatertInformasjon> hentOppfolgingData, AktoerId aktoerId) {
        return !hentOppfolgingData
                .map(oppfolgingData -> veilederHarTilgangTilEnhet(aktoerId, oppfolgingData))
                .getOrElse(false);
    }

    public boolean veilederHarTilgangTilEnhet(AktoerId aktoerId, BrukerOppdatertInformasjon oppfolgingData) {
        String veilederId = oppfolgingData.getVeileder();
        return brukerRepository
                .retrievePersonid(aktoerId)
                .peek(personId -> log.info("PersonId er {}, {}", personId, aktoerId))
                .flatMap(brukerRepository::retrieveNavKontor)
                .peek(enhet -> log.info("Enhet {}, {} ", enhet, aktoerId))
                .map(enhet -> veilarbVeilederClient.hentVeilederePaaEnhet(enhet))
                .peek(veilederePaaEnhet -> log.info("AktoerId {}, Veileder: {} Veileder på enhet: {}", veilederId, veilederePaaEnhet, aktoerId))
                .map(veilederePaaEnhet -> veilederePaaEnhet.contains(veilederId))
                .getOrElse(false);
    }
}
