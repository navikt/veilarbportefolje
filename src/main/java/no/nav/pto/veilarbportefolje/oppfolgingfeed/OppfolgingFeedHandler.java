package no.nav.pto.veilarbportefolje.oppfolgingfeed;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.pto.veilarbportefolje.elastic.MetricsReporter;
import no.nav.pto.veilarbportefolje.feed.consumer.FeedCallback;
import no.nav.pto.veilarbportefolje.arbeidsliste.ArbeidslisteService;
import no.nav.pto.veilarbportefolje.cv.CvService;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktoerId;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.pto.veilarbportefolje.domene.VeilederId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepository;
import no.nav.pto.veilarbportefolje.client.VeilarbVeilederClient;
import no.nav.sbl.jdbc.Transactor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Comparator.naturalOrder;
import static no.nav.pto.veilarbportefolje.elastic.MetricsReporter.getMeterRegistry;
import static no.nav.pto.veilarbportefolje.kafka.KafkaConfig.KAFKA_OPPFOLGING_BEHANDLE_MELDINGER_TOGGLE;

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
    private final CvService cvService;

    public OppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                 BrukerRepository brukerRepository,
                                 ElasticIndexer elasticIndexer,
                                 OppfolgingRepository oppfolgingRepository,
                                 VeilarbVeilederClient veilarbVeilederClient,
                                 Transactor transactor,
                                 CvService cvService,
                                 UnleashService unleashService) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerRepository = brukerRepository;
        this.elasticIndexer = elasticIndexer;
        this.oppfolgingRepository = oppfolgingRepository;
        this.veilarbVeilederClient = veilarbVeilederClient;
        this.transactor = transactor;
        this.unleashService = unleashService;
        this.cvService = cvService;

        Gauge.builder("portefolje_feed_last_id", OppfolgingFeedHandler::getLastEntry).tag("feed_name", FEED_NAME).register(getMeterRegistry());

    }

    private static BigDecimal getLastEntry() {
        return lastEntry;
    }

    @Override
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {

        if (unleashService.isEnabled(KAFKA_OPPFOLGING_BEHANDLE_MELDINGER_TOGGLE)) {
            log.info("Oppdateringer for oppfølgingsstatus blir behandlet via kafka");
            return;
        }

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
        }
    }

    static Optional<BigDecimal> finnMaxFeedId(List<BrukerOppdatertInformasjon> data) {
        return data.stream().map(BrukerOppdatertInformasjon::getFeedId).filter(Objects::nonNull).max(naturalOrder());
    }

    private void oppdaterOppfolgingData(BrukerOppdatertInformasjon oppfolgingData) {
        AktoerId aktoerId = AktoerId.of(oppfolgingData.getAktoerid());

        Try<BrukerOppdatertInformasjon> hentOppfolgingData = oppfolgingRepository.retrieveOppfolgingData(aktoerId);

        boolean skalSletteArbeidsliste = brukerErIkkeUnderOppfolging(oppfolgingData) || eksisterendeVeilederHarIkkeTilgangTilBrukerSinEnhet(hentOppfolgingData, aktoerId);

        if (brukerErIkkeUnderOppfolging(oppfolgingData)) {
            cvService.setHarDeltCvTilNei(aktoerId);
        }

        transactor.inTransaction(() -> {
            if (skalSletteArbeidsliste) {
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
        VeilederId veilederId = VeilederId.of(oppfolgingData.getVeileder());
        return brukerRepository
                .retrievePersonid(aktoerId)
                .flatMap(brukerRepository::retrieveEnhet)
                .map(enhet -> veilarbVeilederClient.hentVeilederePaaEnhet(enhet).contains(veilederId))
                .getOrElse(false);
    }
}
