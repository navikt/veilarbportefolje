package no.nav.fo.consumer;

import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.OppfolgingFeedRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.Oppfolgingstatus;
import no.nav.fo.domene.PersonId;
import no.nav.fo.feed.FeedUtils;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.SolrService;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.fo.feed.FeedUtils.getErUnderOppfolging;
import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.OppfolgingUtils.skalArbeidslisteSlettes;

@Slf4j
public class OppfolgingFeedHandler implements FeedCallback<BrukerOppdatertInformasjon> {

    public static final String OPPFOLGING_SIST_OPPDATERT = "oppfolging_sist_oppdatert";

    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;
    private AktoerService aktoerService;
    private SolrService solrService;
    private OppfolgingFeedRepository oppfolgingFeedRepository;

    @Inject
    public OppfolgingFeedHandler(ArbeidslisteService arbeidslisteService,
                                 BrukerRepository brukerRepository,
                                 AktoerService aktoerService,
                                 SolrService solrService,
                                 OppfolgingFeedRepository oppfolgingFeedRepository) {
        this.arbeidslisteService = arbeidslisteService;
        this.brukerRepository = brukerRepository;
        this.aktoerService = aktoerService;
        this.solrService = solrService;
        this.oppfolgingFeedRepository = oppfolgingFeedRepository;

    }

    @Override
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {
        log.debug(String.format("Feed-data mottatt: %s", data));
        behandleObjektFraFeed(data);
        brukerRepository.updateMetadata(OPPFOLGING_SIST_OPPDATERT, Date.from(ZonedDateTime.parse(lastEntryId).toInstant()));
        Event event = MetricsFactory.createEvent("datamotattfrafeed");
        event.report();
    }

    private void behandleObjektFraFeed(List<BrukerOppdatertInformasjon> brukere) {
        try {
            timed(
                    "feed.oppfolging.objekt",
                    () -> {
                        List<AktoerId> distinctAktoerIds = brukere.stream().map(BrukerOppdatertInformasjon::getAktoerid).map(AktoerId::of).distinct().collect(Collectors.toList());

                        Map<AktoerId, Optional<PersonId>> identMap = aktoerService.hentPersonidsForAktoerids(distinctAktoerIds);

                        insertOppfolgingsinformasjonForBrukere(brukere, identMap);

                        List<PersonId> personIds = FeedUtils.getPresentPersonids(identMap);

                        Map<PersonId, Oppfolgingstatus> oppfolgingstatus = brukerRepository.retrieveOppfolgingstatus(personIds)
                                .getOrElseThrow(() -> new InternalServerErrorException("Kunne ikke finne oppfolgingsstatus for liste av brukere i databasen"));

                        slettArbeidslisteForBrukere(brukere,identMap,oppfolgingstatus);

                        Map<Tuple2<AktoerId, PersonId>, Boolean> aktoerErUndeOppfolging = getErUnderOppfolging(distinctAktoerIds, identMap, oppfolgingstatus);


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
                    (timer, hasFailed) -> timer.addTagToReport("antall", Integer.toString(brukere.size()))
            );
        }catch(Exception e) {
            log.error("Feil ved behandling av oppfølgingsdata (oppfolging) fra feed for liste med brukere.", e);
        }
    }

    private void slettArbeidslisteForBrukere(List<BrukerOppdatertInformasjon> brukere,
                                             Map<AktoerId, Optional<PersonId>> identMap,
                                             Map<PersonId, Oppfolgingstatus> oppfolginsstauser) {
        List<AktoerId> slettArbeidslisteForAktoerids = new ArrayList<>(identMap.keySet().size());
        identMap.keySet().forEach(aktoerId -> {
            BrukerOppdatertInformasjon bruker = brukere.stream().filter(b -> b.getAktoerid().equals(aktoerId.toString())).findFirst().orElse(new BrukerOppdatertInformasjon());
            PersonId personId = identMap.get(aktoerId).orElse(null);
            Oppfolgingstatus oppfolgingstatus = Optional.ofNullable(oppfolginsstauser.get(personId)).orElse(new Oppfolgingstatus());
            if(skalArbeidslisteSlettes(oppfolgingstatus.getVeileder(), bruker.getVeileder(), bruker.getOppfolging())){
                slettArbeidslisteForAktoerids.add(aktoerId);
            }
        });

        arbeidslisteService.deleteArbeidslisteForAktoerids(slettArbeidslisteForAktoerids);
    }

    private void insertOppfolgingsinformasjonForBrukere(List<BrukerOppdatertInformasjon> brukere, Map<AktoerId, Optional<PersonId>> identMap) {
        brukere.forEach(bruker -> insertOppfolgingsinformasjon(bruker, identMap.get(AktoerId.of(bruker.getAktoerid())).orElse(null)));
    }

    private void insertOppfolgingsinformasjon(BrukerOppdatertInformasjon bruker, PersonId personId) {
        if(Objects.isNull(personId)) {
            log.error("Kunne ikke oppdatere oppfolginsinformasjon for aktorid {} når person id er null", bruker.getAktoerid());
            return;
        }
        try {
            timed("oppdater.oppfolgingsinformasjon", ()->oppfolgingFeedRepository.insertVeilederOgOppfolginsinfo(bruker,personId));
        } catch(Exception e) {
            log.error("Kunne ikke oppdatere oppfolgingsinformasjon for aktorid {}", bruker.getAktoerid(), e);
        }
    }
}
