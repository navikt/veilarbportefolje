package no.nav.fo.consumer;


import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Oppfolgingstatus;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.feed.DialogDataFraFeed;
import no.nav.fo.feed.DialogFeedRepository;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.SolrService;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.fo.feed.FeedUtils.getErUnderOppfolging;
import static no.nav.fo.feed.FeedUtils.getPresentPersonids;
import static no.nav.fo.util.MetricsUtils.timed;

@Slf4j
public class DialogDataFeedHandler implements FeedCallback<DialogDataFraFeed> {

    private final AktoerService aktoerService;
    private final BrukerRepository brukerRepository;
    private final SolrService solrService;
    private final DialogFeedRepository dialogFeedRepository;

    @Inject
    public DialogDataFeedHandler(AktoerService aktoerService,
                                 BrukerRepository brukerRepository,
                                 SolrService solrService,
                                 DialogFeedRepository dialogFeedRepository) {
        this.aktoerService = aktoerService;
        this.brukerRepository = brukerRepository;
        this.solrService = solrService;
        this.dialogFeedRepository = dialogFeedRepository;
    }

    @Override
    @Transactional
    public void call(String lastEntry, List<DialogDataFraFeed> data) {
        behandleDialogData(data);
        brukerRepository.updateMetadata("dialogaktor_sist_oppdatert", Date.from(ZonedDateTime.parse(lastEntry).toInstant()));
        Event event = MetricsFactory.createEvent("datamotattfrafeed");
        event.report();
    }

    private void behandleDialogData(List<DialogDataFraFeed> dialoger) {
        try {
            timed("feed.dialog.objekt",
                    () -> {
                        List<AktoerId> distinctAktoerids = getDistinctAktoerids(dialoger);
                        Map<AktoerId, Optional<PersonId>> identMap = aktoerService.hentPersonidsForAktoerids(distinctAktoerids);
                        upsertDialogdataListe(dialoger, identMap);

                        List<PersonId> personIds = getPresentPersonids(identMap);

                        Map<PersonId, Oppfolgingstatus> oppfolgingstatus = brukerRepository.retrieveOppfolgingstatus(personIds)
                                .getOrElseThrow(() -> new InternalServerErrorException("Kunne ikke finne oppfolgingsstatus for liste av brukere i databasen"));

                        Map<Tuple2<AktoerId, PersonId>, Boolean> aktoerErUndeOppfolging = getErUnderOppfolging(distinctAktoerids, identMap, oppfolgingstatus);

                        List<PersonId> ikkeUnderOppfolging = new ArrayList<>();
                        List<PersonId> underOppfolging = new ArrayList<>();
                        aktoerErUndeOppfolging.forEach((key, value) -> {
                            if (value) {
                                underOppfolging.add(key._2);
                            } else {
                                ikkeUnderOppfolging.add(key._2);
                            }
                        });

                        solrService.populerIndeksForPersonids(underOppfolging);
                        brukerRepository.deleteBrukerdataForPersonIds(ikkeUnderOppfolging);
                        solrService.slettBrukere(ikkeUnderOppfolging);
                        solrService.commit();

                    },
                    (timer, hasFailed) -> timer.addTagToReport("antall", Integer.toString(dialoger.size()))
            );
        } catch(Exception e) {
            log.error("Feil ved behandling av dialogdata fra feed for liste med brukere.", e);
        }
    }

    List<AktoerId> getDistinctAktoerids(List<DialogDataFraFeed> dialoger) {
        return dialoger.stream()
                .map(DialogDataFraFeed::getAktorId)
                .distinct()
                .map(AktoerId::of).collect(Collectors.toList());
    }

    private void upsertDialogdataListe(List<DialogDataFraFeed> dialoger, Map<AktoerId, Optional<PersonId>> identMap) {
        dialoger.forEach( dialog -> {
            AktoerId aktoerId = AktoerId.of(dialog.getAktorId());
            upsertDialogdata(dialog, identMap.get(aktoerId).orElse(null));
        });
    }

    private void upsertDialogdata(DialogDataFraFeed dialog, PersonId personId) {
        if(Objects.isNull(personId)) {
            log.error("Klarte ikke å finne  Peprsonid, kunne dermed ikke oppdatere dialogdata for aktoid {}", dialog.getAktorId());
            return;
        }
        dialogFeedRepository.upsertDialogdata(dialog, personId);
    }
}
