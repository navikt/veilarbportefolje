package no.nav.fo.consumer;


import no.nav.fo.database.BrukerRepository;
import no.nav.fo.database.PersistentOppdatering;
import no.nav.fo.domene.Aktivitet.AktivitetBrukerOppdatering;
import no.nav.fo.domene.Aktivitet.AktivitetDTO;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.exception.FantIkkePersonIdException;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.AktoerService;
import no.nav.fo.util.AktivitetUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.LocalDate;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.util.AktivitetUtils.*;
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
            LOG.error("Kunne ikke lagre aktivitetdata fra feed. aktivitetid: {}", aktivitet.getAktivitetId(), e);
        }
    }

    void behandleAktivitetdata( String aktoerid) {
        try {
            timed("feed.aktivitet.indekseraktivitet",
                    () -> {
                        persistentOppdatering.lagre(hentAktivitetBrukerOppdatering(aktoerid, aktoerService, brukerRepository));
                        return null;
                    },
                    (timer, hasFailed) -> { if(hasFailed) { timer.addTagToReport("aktoerhash", DigestUtils.md5Hex(aktoerid).toUpperCase()); }}
            );
        }catch(Exception e) {
            LOG.error("Feil ved behandling av aktivitetdata for aktoerid: {}", aktoerid, e);
        }
    }
}
