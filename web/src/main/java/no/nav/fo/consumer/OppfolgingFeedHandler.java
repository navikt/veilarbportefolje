package no.nav.fo.consumer;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.BrukerOppdatertInformasjon;
import no.nav.fo.domene.Oppfolgingstatus;
import no.nav.fo.domene.PersonId;
import no.nav.fo.exception.FantIkkeOppfolgingsbrukerException;
import no.nav.fo.exception.FantIkkePersonIdException;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.fo.service.SolrService;
import no.nav.fo.util.MetricsUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.apache.commons.codec.digest.DigestUtils;

import javax.inject.Inject;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.List;

import static no.nav.fo.util.OppfolgingUtils.erBrukerUnderOppfolging;
import static no.nav.fo.util.OppfolgingUtils.skalArbeidslisteSlettes;

@Slf4j
public class OppfolgingFeedHandler implements FeedCallback<BrukerOppdatertInformasjon> {

    public static final String OPPFOLGING_SIST_OPPDATERT = "oppfolging_sist_oppdatert";

    private OppdaterBrukerdataFletter oppdaterBrukerdataFletter;
    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;
    private AktoerService aktoerService;
    private SolrService solrService;

    @Inject
    public OppfolgingFeedHandler(OppdaterBrukerdataFletter oppdaterBrukerdataFletter,
                                 ArbeidslisteService arbeidslisteService,
                                 BrukerRepository brukerRepository,
                                 AktoerService aktoerService,
                                 SolrService solrService) {
        this.oppdaterBrukerdataFletter = oppdaterBrukerdataFletter;
        this.arbeidslisteService = arbeidslisteService;
        this.brukerRepository = brukerRepository;
        this.aktoerService = aktoerService;
        this.solrService = solrService;

    }

    @Override
    public void call(String lastEntryId, List<BrukerOppdatertInformasjon> data) {
        log.debug(String.format("Feed-data mottatt: %s", data));
        data.forEach(this::behandleObjektFraFeed);
        brukerRepository.updateMetadata(OPPFOLGING_SIST_OPPDATERT, Date.from(ZonedDateTime.parse(lastEntryId).toInstant()));
        Event event = MetricsFactory.createEvent("datamotattfrafeed");
        event.report();
    }

    private void behandleObjektFraFeed(BrukerOppdatertInformasjon bruker) {
        try {
            MetricsUtils.timed(
                    "feed.oppfolging.objekt",
                    () -> {
                        AktoerId aktoerId = AktoerId.of(bruker.getAktoerid());
                        PersonId personId = aktoerService.hentPersonidFraAktoerid(aktoerId)
                                .getOrElseThrow(() -> new FantIkkePersonIdException(aktoerId));

                        Oppfolgingstatus oppfolgingstatus = brukerRepository.retrieveOppfolgingstatus(personId)
                                .getOrElseThrow(() -> new FantIkkeOppfolgingsbrukerException(personId))
                                .setOppfolgingsbruker(bruker.getOppfolging());

                        if(skalArbeidslisteSlettes(oppfolgingstatus.getVeileder(), bruker.getVeileder(), bruker.getOppfolging())) {
                            arbeidslisteService.deleteArbeidsliste(AktoerId.of(bruker.getAktoerid()));
                        }
                        if (erBrukerUnderOppfolging(oppfolgingstatus.getFormidlingsgruppekode(), oppfolgingstatus.getServicegruppekode(),bruker.getOppfolging())) {
                            oppdaterBrukerdataFletter.oppdaterOppfolgingForBruker(bruker, personId);
                        } else {
                            brukerRepository.deleteBrukerdata(personId);
                            solrService.slettBruker(personId);
                            solrService.commit();
                        }
                    },
                    (timer, hasFailed) -> { if(hasFailed) {timer.addTagToReport("aktorhash", DigestUtils.md5Hex(bruker.getAktoerid()).toUpperCase());}}
            );
        }catch(Exception e) {
            log.error("Feil ved behandling av oppfølgingsdata (oppfolging) fra feed for aktorid {}.", bruker.getAktoerid(), e);
        }
    }
}
