package no.nav.fo.consumer;


import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.AktivitetService;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.MetricsUtils.timed;
import static org.slf4j.LoggerFactory.getLogger;

public class AktivitetFeedHandler implements FeedCallback<AktivitetDataFraFeed> {

    private static final Logger LOG = getLogger(AktivitetFeedHandler.class);

    private BrukerRepository brukerRepository;
    private AktivitetService aktivitetService;

    @Inject
    public AktivitetFeedHandler(BrukerRepository brukerRepository, AktivitetService aktivitetService) {
        this.brukerRepository = brukerRepository;
        this.aktivitetService = aktivitetService;
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
            LOG.error("Kunne ikke lagre aktivitetdata fra feed. aktivitetid: {}", aktivitet.getAktivitetId(), e);
        }
    }

    void behandleAktivitetdata( String aktoerid) {
        try {
            timed("feed.aktivitet.indekseraktivitet",
                    () -> {
                        aktivitetService.utledOgIndekserAktivitetstatuserForAktoerid(aktoerid);
                        return null;
                    },
                    (timer, hasFailed) -> { if(hasFailed) { timer.addTagToReport("aktoerhash", DigestUtils.md5Hex(aktoerid).toUpperCase()); }}
            );
        }catch(Exception e) {
            LOG.error("Feil ved behandling av aktivitetdata for aktoerid: {}", aktoerid, e);
        }
    }
}
