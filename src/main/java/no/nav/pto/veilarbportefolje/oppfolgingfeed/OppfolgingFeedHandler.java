package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import io.micrometer.core.instrument.Gauge;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.pto.veilarbportefolje.database.Transactor;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedCallback;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Comparator.naturalOrder;
import static no.nav.pto.veilarbportefolje.elastic.MetricsReporter.getMeterRegistry;
import static no.nav.common.utils.IdUtils.generateId;

@Slf4j
public class OppfolgingFeedHandler implements FeedCallback {


    private static final String MDC_KEY = "oppfolging_data";
    private static final String FEED_NAME = "oppfolging";

    private static BigDecimal lastEntry;
    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;
    private ElasticIndexer elasticIndexer;
    private OppfolgingRepository oppfolgingRepository;
    private VeilarbVeilederClient veilarbVeilederClient;
    private Transactor transactor;
    private final UnleashService unleashService;

    public OppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                 BrukerRepository brukerRepository,
                                 ElasticIndexer elasticIndexer,
                                 OppfolgingRepository oppfolgingRepository,
                                 VeilarbVeilederClient veilarbVeilederClient,
                                 Transactor transactor,
                                 UnleashService unleashService) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerRepository = brukerRepository;
        this.elasticIndexer = elasticIndexer;
        this.oppfolgingRepository = oppfolgingRepository;
        this.veilarbVeilederClient = veilarbVeilederClient;
        this.transactor = transactor;
        this.unleashService = unleashService;

        Gauge.builder("portefolje_feed_last_id", OppfolgingFeedHandler::getLastEntry).tag("feed_name", FEED_NAME).register(getMeterRegistry());
    }

    private static BigDecimal getLastEntry() {
        return lastEntry;
    }

    @Override
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {

        if (unleashService.isEnabled(FeatureToggle.KAFKA_OPPFOLGING_BEHANDLE_MELDINGER)) {
            log.info("Oppdateringer for oppfølgingsstatus blir behandlet via kafka");
            return;
        }


        MDC.put(MDC_KEY, generateId());
        log.info("OppfolgingerfeedDebug data: {}", data);

        try {

            data.forEach(info -> {
                if (info.getStartDato() == null) {
                    log.warn("Bruker {} har ingen startdato", info.getAktoerid());
                }
                oppdaterOppfolgingData(info);

                elasticIndexer.indekserAsynkront(AktoerId.of(info.getAktoerid()));
            });

            finnMaxFeedId(data).ifPresent(id -> {
                oppfolgingRepository.updateOppfolgingFeedId(id);
                lastEntry = id;
            });

        } catch (Exception e) {
            String message = "Feil ved behandling av oppfølgingsdata (oppfolging) fra feed for liste med brukere.";
            log.error(message, e);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    static Optional<BigDecimal> finnMaxFeedId(List<BrukerOppdatertInformasjon> data) {
        return data.stream().map(BrukerOppdatertInformasjon::getFeedId).filter(Objects::nonNull).max(naturalOrder());
    }

    private void oppdaterOppfolgingData(BrukerOppdatertInformasjon oppfolgingData) {
        AktoerId aktoerId = AktoerId.of(oppfolgingData.getAktoerid());

        Try<BrukerOppdatertInformasjon> hentOppfolgingData = oppfolgingRepository.retrieveOppfolgingData(aktoerId);

        boolean skalSletteArbeidsliste = brukerErIkkeUnderOppfolging(oppfolgingData) || eksisterendeVeilederHarIkkeTilgangTilBrukerSinEnhet(hentOppfolgingData, aktoerId);

        transactor.inTransaction(() -> {
            if (skalSletteArbeidsliste) {
                log.info("Sletter arbeidsliste for bruker {}", aktoerId);
                arbeidslisteService.deleteArbeidslisteForAktoerId(aktoerId);
            }
            oppfolgingRepository.oppdaterOppfolgingData(oppfolgingData);
        });

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
                .flatMap(brukerRepository::retrieveEnhet)
                .map(enhet -> veilarbVeilederClient.hentVeilederePaaEnhet(enhet))
                .peek(veilederePaaEnhet -> log.info("Veileder: {} Veileder på enhet: {}", veilederId, veilederePaaEnhet))
                .map(veilederePaaEnhet -> veilederePaaEnhet.contains(veilederId))
                .getOrElse(false);
    }
}
