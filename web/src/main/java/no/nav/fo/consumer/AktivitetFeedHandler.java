package no.nav.fo.consumer;


import javaslang.Tuple2;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktivitetData;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.feed.consumer.FeedCallback;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.domene.AktivitetData.aktivitettyperSet;
import static no.nav.fo.util.AktivitetUtils.erBrukersAktivitetAktiv;
import static org.slf4j.LoggerFactory.getLogger;

public class AktivitetFeedHandler implements FeedCallback<AktivitetDataFraFeed> {

    private static final Logger LOG = getLogger(AktivitetFeedHandler.class);

    @Inject
    private BrukerRepository brukerRepository;

    @Override
    public void call(String lastEntry, List<AktivitetDataFraFeed> data) {

        data.forEach((aktivitet) -> brukerRepository.upsertAktivitet(aktivitet));

        data.stream().map(AktivitetDataFraFeed::getAktorId)
                .distinct()
                .collect(toList())
                    .forEach(aktoerid -> oppdaterAktivitetstatusForBruker(brukerRepository.getAktiviteterForAktoerid(aktoerid), aktoerid));
        brukerRepository.setAktiviteterSistOppdatert(lastEntry);
    }

    void oppdaterAktivitetstatusForBruker(List<Tuple2<String, String>> aktivitetStatus, String aktoerid) {
        Map<String, Boolean> aktivitetTypeTilStatus = new HashMap<>();

        aktivitettyperSet.forEach(aktivitetsype -> {
            List<String> statuser = aktivitetStatus
                    .stream()
                    .filter(tuple -> aktivitetsype.equals(tuple._1))
                    .map(tuple -> tuple._2)
                    .collect(toList());

            aktivitetTypeTilStatus.put(aktivitetsype, erBrukersAktivitetAktiv(statuser, AktivitetData.fullførteStatuser));
        });

        aktivitetTypeTilStatus.forEach((type, status) -> brukerRepository.upsertAktivitetStatuserForBruker(type, status, aktoerid));
    }
}
