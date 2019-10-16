package no.nav.fo.veilarbportefolje.consumer;

import io.micrometer.core.instrument.Counter;
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

    private final Counter antallTotaltMetrikk;
    private final Counter antallFeiletMetrikk;

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

        antallTotaltMetrikk = Counter.builder("portefolje_feed_oppfolging").register(getMeterRegistry());
        antallFeiletMetrikk = Counter.builder("portefolje_feed_oppfolging_feilet").register(getMeterRegistry());
    }

    @Override
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {
        try {

            log.info("OppfolgingerfeedDebug data: {}", data);

            data.forEach(info -> {
                oppdaterOppfolgingData(info);
                elasticIndexer.indekserAsynkront(AktoerId.of(info.getAktoerid()));
                antallTotaltMetrikk.increment();
            });

            finnMaxFeedId(data).ifPresent(id -> oppfolgingFeedRepository.updateOppfolgingFeedId(id));

        } catch (Exception e) {
            String message = "Feil ved behandling av oppfølgingsdata (oppfolging) fra feed for liste med brukere.";
            log.error(message, e);
            antallFeiletMetrikk.increment();
        }
    }

    static Optional<BigDecimal> finnMaxFeedId(List<BrukerOppdatertInformasjon> data) {
        return data.stream().map(BrukerOppdatertInformasjon::getFeedId).filter(Objects::nonNull).max(naturalOrder());
    }

    private void oppdaterOppfolgingData(BrukerOppdatertInformasjon info) {
        boolean slettes = !info.getOppfolging() ||
                !bytterTilVeilederPaSammeEnhet(AktoerId.of(info.getAktoerid()));

        transactor.inTransaction(() -> {
            if (slettes) {
                arbeidslisteService.deleteArbeidslisteForAktoerid(AktoerId.of(info.getAktoerid()));
            }
            oppfolgingFeedRepository.oppdaterOppfolgingData(info);
        });

    }

    private Boolean bytterTilVeilederPaSammeEnhet(AktoerId aktoerId) {
        return oppfolgingFeedRepository.retrieveOppfolgingData(aktoerId.toString())
                .map(oppfolgingData -> brukerRepository
                        .retrievePersonid(aktoerId)
                        .flatMap(personId -> brukerRepository.retrieveEnhet(personId))
                        .map(enhet -> veilederService.getIdenter(enhet)
                                .contains(VeilederId.of(oppfolgingData.getVeileder()))
                        ).getOrElse(false)
                ).getOrElse(false);
    }
}
