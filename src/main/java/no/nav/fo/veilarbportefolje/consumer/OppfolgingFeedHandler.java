package no.nav.fo.veilarbportefolje.consumer;

import io.micrometer.core.instrument.Counter;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.veilarbportefolje.database.BrukerRepository;
import no.nav.fo.veilarbportefolje.database.OppfolgingFeedRepository;
import no.nav.fo.veilarbportefolje.domene.AktoerId;
import no.nav.fo.veilarbportefolje.domene.BrukerOppdatertInformasjon;
import no.nav.fo.veilarbportefolje.domene.VeilederId;
import no.nav.fo.veilarbportefolje.indeksering.IndekseringService;
import no.nav.fo.veilarbportefolje.service.ArbeidslisteService;
import no.nav.fo.veilarbportefolje.service.VeilederService;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.jdbc.Transactor;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.naturalOrder;

@Slf4j
public class OppfolgingFeedHandler implements FeedCallback<BrukerOppdatertInformasjon> {

    public static final String OPPFOLGING_SIST_OPPDATERT = "oppfolging_sist_oppdatert";
    private final Counter counter;
    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;
    private IndekseringService indekseringService;
    private OppfolgingFeedRepository oppfolgingFeedRepository;
    private VeilederService veilederService;
    private Transactor transactor;

    @Inject
    public OppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                 BrukerRepository brukerRepository,
                                 IndekseringService indekseringService,
                                 OppfolgingFeedRepository oppfolgingFeedRepository,
                                 VeilederService veilederService,
                                 Transactor transactor) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerRepository = brukerRepository;
        this.indekseringService = indekseringService;
        this.oppfolgingFeedRepository = oppfolgingFeedRepository;
        this.veilederService = veilederService;
        this.transactor = transactor;

        this.counter = Counter.builder("portefolje_feed_oppfolgingsbrukere").register(MetricsFactory.getMeterRegistry());

    }

    @Override
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> brukerInfo) {

        try {
            brukerInfo.forEach(info -> {
                oppdaterOppfolgingData(info);
                indekseringService.indekserAsynkront(AktoerId.of(info.getAktoerid()));
            });
            parseLastEntryIdToDate(lastEntryId).ifPresent(id -> brukerRepository.updateMetadata(OPPFOLGING_SIST_OPPDATERT, id));
            finnMaxFeedId(brukerInfo).ifPresent(id -> oppfolgingFeedRepository.updateOppfolgingFeedId(id));

        } catch (Exception e) {
            log.error("Feil ved behandling av oppfølgingsdata (oppfolging) fra feed for {}",brukerInfo, e);
            throw e;
        }
        counter.increment();
    }

    static Optional<BigDecimal> finnMaxFeedId(List<BrukerOppdatertInformasjon> data) {
        return data.stream().map(BrukerOppdatertInformasjon::getFeedId).filter(i -> i != null).max(naturalOrder());
    }

    private void oppdaterOppfolgingData(BrukerOppdatertInformasjon info) {
        boolean slettes = !info.getOppfolging() ||
                !bytterTilVeilederPaSammeEnhet(AktoerId.of(info.getAktoerid()));

        log.info("Oppdaterer oppfolgingsdata for bruker med aktoerid {}", info.getAktoerid());

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
