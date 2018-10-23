package no.nav.fo.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.OppfolgingFeedRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.VeilederId;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.SolrService;
import no.nav.fo.service.VeilederService;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.jdbc.Transactor;

import javax.inject.Inject;

import io.vavr.control.Try;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.naturalOrder;
import static no.nav.fo.util.MetricsUtils.timed;

@Slf4j
public class OppfolgingFeedHandler implements FeedCallback<BrukerOppdatertInformasjon> {

    public static final String OPPFOLGING_SIST_OPPDATERT = "oppfolging_sist_oppdatert";

    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;
    private SolrService solrService;
    private OppfolgingFeedRepository oppfolgingFeedRepository;
    private VeilederService veilederService;
    private Transactor transactor;

    @Inject
    public OppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                 BrukerRepository brukerRepository,
                                 SolrService solrService,
                                 OppfolgingFeedRepository oppfolgingFeedRepository,
                                 VeilederService veilederService,
                                 Transactor transactor) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerRepository = brukerRepository;
        this.solrService = solrService;
        this.oppfolgingFeedRepository = oppfolgingFeedRepository;
        this.veilederService = veilederService;
        this.transactor = transactor;
    }

    @Override
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {

        try {
            timed("feed.oppfolging.objekt", () -> {
                        log.info("OppfolgingerfeedDebug data: {}", data);

                        data.forEach(info -> {
                            oppdaterOppfolgingData(info);
                            solrService.indekserAsynkront(AktoerId.of(info.getAktoerid()));
                        });
                        parseLastEntryIdToDate(lastEntryId).ifPresent(id -> brukerRepository.updateMetadata(OPPFOLGING_SIST_OPPDATERT, id));
                        finnMaxFeedId(data).ifPresent(id ->  oppfolgingFeedRepository.updateOppfolgingFeedId(id));
                    },
                    (timer, hasFailed) -> timer.addTagToReport("antall", Integer.toString(data.size())));
        } catch (Exception e) {
            log.error("Feil ved behandling av oppfølgingsdata (oppfolging) fra feed for liste med brukere.", e);
        }

        MetricsFactory.createEvent("datamotattfrafeed").report();
    }

    static Optional<java.util.Date> parseLastEntryIdToDate(String lastEntryId) {
        return Try.of(() -> Date.from(ZonedDateTime.parse(lastEntryId).toInstant())).toJavaOptional();
    }

    static Optional<BigDecimal> finnMaxFeedId(List<BrukerOppdatertInformasjon> data) {
        return data.stream().map(BrukerOppdatertInformasjon::getFeedId).filter(i -> i != null).max(naturalOrder());
    }

    private void oppdaterOppfolgingData(BrukerOppdatertInformasjon info) {
        boolean slettes = !info.getOppfolging() ||
                !bytterTilVeilederPaSammeEnhet(AktoerId.of(info.getAktoerid()));

        transactor.inTransaction(() -> {
            if (slettes) {
                arbeidslisteService.deleteArbeidslisteForAktoerid(AktoerId.of(info.getAktoerid()));
            }
            timed("oppdater.oppfolgingsinformasjon", () -> oppfolgingFeedRepository.oppdaterOppfolgingData(info));
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
