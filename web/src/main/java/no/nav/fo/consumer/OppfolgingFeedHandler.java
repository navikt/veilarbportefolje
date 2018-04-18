package no.nav.fo.consumer;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.OppfolgingFeedRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.SolrService;
import no.nav.metrics.MetricsFactory;
import no.nav.sbl.jdbc.Transactor;

import javax.inject.Inject;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.*;

import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.OppfolgingUtils.skalArbeidslisteSlettes;

@Slf4j
public class OppfolgingFeedHandler implements FeedCallback<BrukerOppdatertInformasjon> {

    public static final String OPPFOLGING_SIST_OPPDATERT = "oppfolging_sist_oppdatert";

    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;
    private SolrService solrService;
    private OppfolgingFeedRepository oppfolgingDataRepository;
    private Transactor transactor;
    
    @Inject
    public OppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                 BrukerRepository brukerRepository,
                                 SolrService solrService,
                                 OppfolgingFeedRepository oppfolgingFeedRepository,
                                 Transactor transactor) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerRepository = brukerRepository;
        this.solrService = solrService;
        this.oppfolgingDataRepository = oppfolgingFeedRepository;
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
                brukerRepository.updateMetadata(OPPFOLGING_SIST_OPPDATERT, Date.from(ZonedDateTime.parse(lastEntryId).toInstant()));
            }, 
            (timer, hasFailed) -> timer.addTagToReport("antall", Integer.toString(data.size())));
        } catch (Exception e) {
            log.error("Feil ved behandling av oppfølgingsdata (oppfolging) fra feed for liste med brukere.", e);
        }

        MetricsFactory.createEvent("datamotattfrafeed").report();
    }

    private void oppdaterOppfolgingData(BrukerOppdatertInformasjon info) {
        Try<BrukerOppdatertInformasjon> eksisterendeData = oppfolgingDataRepository.retrieveOppfolgingData(info.getAktoerid());
        String eksisterendeVeileder = eksisterendeData.isSuccess() ? eksisterendeData.get().getVeileder() : null;

        transactor.inTransaction(() -> {                
            if(skalArbeidslisteSlettes(eksisterendeVeileder, info.getVeileder(), info.getOppfolging())) {
                arbeidslisteService.deleteArbeidslisteForAktoerid(AktoerId.of(info.getAktoerid()));
            }
            timed("oppdater.oppfolgingsinformasjon", ()->oppfolgingDataRepository.oppdaterOppfolgingData(info));
        });

    }
}
