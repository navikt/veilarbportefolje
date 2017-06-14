package no.nav.fo.consumer;


import javaslang.Tuple2;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.BrukerOppdatering;
import no.nav.fo.domene.Brukerdata;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.exception.FantIkkePersonIdException;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.OppdaterBrukerdataFletter;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.domene.Aktivitet.AktivitetData.aktivitetTyperList;
import static no.nav.fo.util.AktivitetUtils.erBrukersAktivitetAktiv;
import static no.nav.fo.util.MetricsUtils.timed;
import static org.slf4j.LoggerFactory.getLogger;

public class AktivitetFeedHandler implements FeedCallback<AktivitetDataFraFeed> {

    private static final Logger LOG = getLogger(AktivitetFeedHandler.class);

    private BrukerRepository brukerRepository;
    private PersistentOppdatering persistentOppdatering;
    private AktoerService aktoerService;

    @Inject
    public AktivitetFeedHandler(BrukerRepository brukerRepository, PersistentOppdatering persistentOppdatering, AktoerService aktoerService) {
        this.brukerRepository = brukerRepository;
        this.persistentOppdatering = persistentOppdatering;
        this.aktoerService = aktoerService;
    }

    @Override
    public void call(String lastEntry, List<AktivitetDataFraFeed> data) {
        List<AktivitetDataFraFeed> avtalteAktiviteter = data
                .stream()
                .filter(AktivitetDataFraFeed::isAvtalt)
                .collect(toList());

        avtalteAktiviteter.forEach(this::lagreAktivitetData);

        avtalteAktiviteter
                .stream().map(AktivitetDataFraFeed::getAktorId)
                .distinct()
                .collect(toList())
                .forEach(this::behandleAktivitetdata);

        brukerRepository.setAktiviteterSistOppdatert(lastEntry);
    }

    void lagreAktivitetData(AktivitetDataFraFeed aktivitet) {
        try{
            timed(
                    "feed.aktivitet.objekt",
                    () -> { brukerRepository.upsertAktivitet(aktivitet); return null;},
                    (timer, hasFailed) -> { if(hasFailed) { timer.addTagToReport("aktoerhash", DigestUtils.md5Hex(aktivitet.getAktorId()).toUpperCase()); }}
                    );
        }catch(Exception e) {
            LOG.error("Kunne ikke lagre aktivitetdata fra feed. aktivitetid: {}, {}", aktivitet.getAktivitetId(), e.getMessage());
        }
    }

    void behandleAktivitetdata( String aktoerid) {
        try {
            timed("feed.aktivitet.indekseraktivitet",
                    () -> { oppdaterAktivitetstatusForBruker(brukerRepository.getAktiviteterForAktoerid(aktoerid), aktoerid); return null; },
                    (timer, hasFailed) -> { if(hasFailed) { timer.addTagToReport("aktoerhash", DigestUtils.md5Hex(aktoerid).toUpperCase()); }}
            );
        }catch(Exception e) {
            LOG.error("Feil ved behandling av aktivitetdata for aktoerid: {}  {}", aktoerid, e.getMessage());
        }
    }

    void oppdaterAktivitetstatusForBruker(List<Tuple2<String, String>> aktivitetStatus, String aktoerid) {
        Map<String, Boolean> aktivitetTypeTilStatus = new HashMap<>();

        aktivitetTyperList.forEach(aktivitetsype -> {
            List<String> statuser = aktivitetStatus
                    .stream()
                    .filter(tuple -> aktivitetsype.toString().equals(tuple._1))
                    .map(tuple -> tuple._2)
                    .collect(toList());

            aktivitetTypeTilStatus.put(aktivitetsype.toString(), erBrukersAktivitetAktiv(statuser));
        });

        String personid = aktoerService.hentPersonidFraAktoerid(aktoerid).orElseThrow( () -> {
            getLogger(OppdaterBrukerdataFletter.class).warn("Fant ikke personid for aktoerid {} ", aktoerid);
            return new FantIkkePersonIdException(aktoerid);
        });
        persistentOppdatering.lagre(new AktivitetsDataEndring(personid, aktoerid, aktivitetTypeTilStatus));
    }

    class AktivitetsDataEndring implements BrukerOppdatering {
        private final String personid;
        private final String aktoerid;
        private Map<String, Boolean> aktivitetStatus;

        public AktivitetsDataEndring(String personid, String aktoerid, Map<String, Boolean> aktivitetStatus) {
            this.personid = personid;
            this.aktoerid = aktoerid;
            this.aktivitetStatus = aktivitetStatus;
        }

        @Override
        public String getPersonid() {
            return personid;
        }

        @Override
        public Brukerdata applyTo(Brukerdata bruker) {
            return bruker
                    .setAktivitetStatus(aktivitetStatus)
                    .setAktoerid(aktoerid);
        }
    }
}
