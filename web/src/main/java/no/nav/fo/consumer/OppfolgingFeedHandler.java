package no.nav.fo.consumer;

import io.vavr.Tuple;
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
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.fo.feed.FeedUtils.finnBrukere;
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
        log.info("OppfolgingerfeedDebug data: {}", data);
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
                        List<AktoerId> unikeAktoerIds = finnUnikeAktoerIds(brukere);
                        Map<AktoerId, Optional<PersonId>> identMap = aktoerService.hentPersonidsForAktoerids(unikeAktoerIds);

                        List<PersonId> personIds = FeedUtils.getPresentPersonids(identMap);

                        Map<PersonId, Oppfolgingstatus> oppfolgingstatus = brukerRepository.retrieveOppfolgingstatus(personIds);

                        insertOppfolgingsinformasjonForBrukere(brukere, identMap);

                        slettArbeidslisteForBrukere(brukere, identMap, oppfolgingstatus);

                        Map<PersonId, Oppfolgingstatus> oppfolgingstatusMedOppdatertInfo = oppdaterMedOppfolgingsFlagg(identMap, oppfolgingstatus, brukere);

                        Map<Boolean, List<Tuple2<AktoerId, PersonId>>> aktoerErUndeOppfolging = getErUnderOppfolging(unikeAktoerIds, identMap, oppfolgingstatusMedOppdatertInfo);
                        List<PersonId> personIdUnderOppfolging = finnBrukere(aktoerErUndeOppfolging, Boolean.TRUE, Tuple2::_2);
                        List<PersonId> personIdIkkeUnderOppfolging = finnBrukere(aktoerErUndeOppfolging, Boolean.FALSE, Tuple2::_2);

                        solrService.populerIndeksForPersonids(personIdUnderOppfolging);
                        brukerRepository.deleteBrukerdataForPersonIds(personIdIkkeUnderOppfolging);
                        solrService.slettBrukere(personIdIkkeUnderOppfolging);
                        solrService.commit();
                    },
                    (timer, hasFailed) -> timer.addTagToReport("antall", Integer.toString(brukere.size()))
            );
        } catch (Exception e) {
            log.error("Feil ved behandling av oppfølgingsdata (oppfolging) fra feed for liste med brukere.", e);
        }
    }

    private Map<PersonId, Oppfolgingstatus> oppdaterMedOppfolgingsFlagg(
            Map<AktoerId, Optional<PersonId>> identMap,
            Map<PersonId, Oppfolgingstatus> oppfolgingstatus,
            List<BrukerOppdatertInformasjon> brukere
    ) {
        Map<PersonId, BrukerOppdatertInformasjon> brukerMap = brukere
                .stream()
                .map((bruker) -> Tuple.of(identMap.get(AktoerId.of(bruker.getAktoerid())), bruker))
                .filter((tuple) -> tuple._1.isPresent())
                .map((tuple) -> Tuple.of(tuple._1.get(), tuple._2))
                .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));

        return oppfolgingstatus
                .entrySet()
                .stream()
                .map((entry) -> {
                    BrukerOppdatertInformasjon brukerOppdatertInformasjon = brukerMap.get(entry.getKey());
                    Oppfolgingstatus oppfolgingsstatus = entry.getValue();
                    Oppfolgingstatus nyOppfolgingsstatus = new Oppfolgingstatus()
                            .setFormidlingsgruppekode(oppfolgingsstatus.getFormidlingsgruppekode())
                            .setServicegruppekode(oppfolgingsstatus.getServicegruppekode())
                            .setVeileder(oppfolgingsstatus.getVeileder())
                            .setOppfolgingsbruker(brukerOppdatertInformasjon.getOppfolging());

                    return Tuple.of(entry.getKey(), nyOppfolgingsstatus);
                })
                .collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));
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
            log.info("Kunne ikke oppdatere oppfolginsinformasjon for aktorid {} når person id er null", bruker.getAktoerid());
            return;
        }
        try {
            timed("oppdater.oppfolgingsinformasjon", ()->oppfolgingFeedRepository.insertVeilederOgOppfolginsinfo(bruker,personId));
        } catch(Exception e) {
            log.error("Kunne ikke oppdatere oppfolgingsinformasjon for aktorid {}", bruker.getAktoerid(), e);
        }
    }

    private static List<AktoerId> finnUnikeAktoerIds(List<BrukerOppdatertInformasjon> brukere) {
        return brukere.stream()
                .map(BrukerOppdatertInformasjon::getAktoerid)
                .map(AktoerId::of)
                .distinct()
                .collect(Collectors.toList());
    }
}
