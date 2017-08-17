package no.nav.fo.consumer;

import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.*;
import no.nav.fo.exception.FantIkkeOppfolgingsbrukerException;
import no.nav.fo.exception.FantIkkePersonIdException;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.ArbeidslisteService;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import no.nav.fo.service.SolrService;
import no.nav.fo.util.OppfolgingUtils;
import no.nav.fo.util.MetricsUtils;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.sql.Date;
import java.time.ZonedDateTime;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class SituasjonFeedHandler implements FeedCallback<BrukerOppdatertInformasjon> {

    private static final Logger LOG = getLogger(SituasjonFeedHandler.class);

    public static final String SITUASJON_SIST_OPPDATERT = "situasjon_sist_oppdatert";

    private OppdaterBrukerdataFletter oppdaterBrukerdataFletter;
    private ArbeidslisteService arbeidslisteService;
    private BrukerRepository brukerRepository;
    private AktoerService aktoerService;
    private SolrService solrService;

    @Inject
    public SituasjonFeedHandler(OppdaterBrukerdataFletter oppdaterBrukerdataFletter,
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
        LOG.debug(String.format("Feed-data mottatt: %s", data));
        data.forEach(this::behandleObjektFraFeed);
        brukerRepository.updateMetadata(SITUASJON_SIST_OPPDATERT, Date.from(ZonedDateTime.parse(lastEntryId).toInstant()));
        Event event = MetricsFactory.createEvent("datamotattfrafeed");
        event.report();
    }

    private void behandleObjektFraFeed(BrukerOppdatertInformasjon bruker) {
        try {
            MetricsUtils.timed(
                    "feed.situasjon.objekt",
                    () -> {
                        AktoerId aktoerId = new AktoerId(bruker.getAktoerid());
                        PersonId personId = aktoerService.hentPersonidFraAktoerid(aktoerId)
                                .getOrElseThrow(() -> new FantIkkePersonIdException(aktoerId));

                        Oppfolgingstatus oppfolgingstatus = brukerRepository.retrieveOppfolgingstatus(personId)
                                .getOrElseThrow(() -> new FantIkkeOppfolgingsbrukerException(personId))
                                .setOppfolgingsbruker(bruker.getOppfolging());

                        if(OppfolgingUtils.skalArbeidslisteSlettes(oppfolgingstatus.getVeileder(), bruker.getVeileder(), bruker.getOppfolging())) {
                            arbeidslisteService.deleteArbeidsliste(new AktoerId(bruker.getAktoerid()));
                        }
                        if(!OppfolgingUtils.erBrukerUnderOppfolging(oppfolgingstatus.getFormidlingsgruppekode(), oppfolgingstatus.getServicegruppekode(),bruker.getOppfolging())) {
                            brukerRepository.deleteBrukerdata(personId);
                            solrService.slettBruker(personId);
                            solrService.commit();
                            return null;
                        }
                        oppdaterBrukerdataFletter.oppdaterSituasjonForBruker(bruker, personId);
                        return null;
                        },
                    (timer, hasFailed) -> { if(hasFailed) {timer.addTagToReport("aktorhash", DigestUtils.md5Hex(bruker.getAktoerid()).toUpperCase());}}
            );
        }catch(Exception e) {
            LOG.error("Feil ved behandling av objekt fra feed med aktorid {}, {}", bruker.getAktoerid(), e.getMessage());
        }
    }
}
