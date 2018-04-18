package no.nav.fo.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.feed.DialogFeedRepository;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.SolrService;
import no.nav.metrics.MetricsFactory;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.*;

import static no.nav.fo.util.MetricsUtils.timed;

@Slf4j
public class NyDialogDataFeedHandler implements FeedCallback<DialogDataFraFeed> {

    private final BrukerRepository brukerRepository;
    private final SolrService solrService;
    private final DialogFeedRepository dialogFeedRepository;

    @Inject
    public NyDialogDataFeedHandler(BrukerRepository brukerRepository,
                                 SolrService solrService,
                                 DialogFeedRepository dialogFeedRepository) {
        this.brukerRepository = brukerRepository;
        this.solrService = solrService;
        this.dialogFeedRepository = dialogFeedRepository;
    }

    @Override
    @Transactional
    public void call(String lastEntry, List<DialogDataFraFeed> data) {

        try {
            timed("feed.dialog.objekt.ny",
                    () -> {
                        data.forEach(info -> {
                            dialogFeedRepository.oppdaterDialogInfoForBruker(info);
                            solrService.indekserAsynkront(AktoerId.of(info.getAktorId()));
                        });
                    },
                    (timer, hasFailed) -> timer.addTagToReport("antall", Integer.toString(data.size()))
            );
            brukerRepository.updateMetadata("dialogaktor_sist_oppdatert", Date.from(ZonedDateTime.parse(lastEntry).toInstant()));
        } catch(Exception e) {
            log.error("Feil ved behandling av dialogdata fra feed for liste med brukere.", e);
        }

        MetricsFactory.createEvent("datamotattfrafeed").report();
    }
}
