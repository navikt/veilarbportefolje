package no.nav.fo.consumer;


import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktoerId;
import no.nav.fo.domene.Oppfolgingstatus;
import no.nav.fo.domene.PersonId;
import no.nav.fo.domene.feed.AktivitetDataFraFeed;
import no.nav.fo.feed.consumer.FeedCallback;
import no.nav.fo.service.AktivitetService;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.SolrService;
import org.apache.commons.codec.digest.DigestUtils;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.OppfolgingUtils.erBrukerUnderOppfolging;

@Slf4j
public class AktivitetFeedHandler implements FeedCallback<AktivitetDataFraFeed> {

    private BrukerRepository brukerRepository;
    private AktivitetService aktivitetService;
    private AktoerService aktoerService;
    private SolrService solrService;
    private AktivitetDAO aktivitetDAO;

    @Inject
    public AktivitetFeedHandler(BrukerRepository brukerRepository,
                                AktivitetService aktivitetService,
                                AktoerService aktoerService,
                                SolrService solrService,
                                AktivitetDAO aktivitetDAO) {
        this.brukerRepository = brukerRepository;
        this.aktivitetService = aktivitetService;
        this.aktoerService = aktoerService;
        this.solrService = solrService;
        this.aktivitetDAO = aktivitetDAO;
    }

    @Override
    public void call(String lastEntry, List<AktivitetDataFraFeed> data) {
        List<AktivitetDataFraFeed> avtalteAktiviteter = data
                .stream()
                .filter(AktivitetDataFraFeed::isAvtalt)
                .collect(toList());

        avtalteAktiviteter.forEach(this::lagreAktivitetData);

        behandleAktivitetdata(avtalteAktiviteter
                .stream().map(AktivitetDataFraFeed::getAktorId)
                .distinct()
                .map(AktoerId::of)
                .collect(toList()));


        brukerRepository.setAktiviteterSistOppdatert(lastEntry);
    }

    private void lagreAktivitetData(AktivitetDataFraFeed aktivitet) {
        try {
            timed(
                    "feed.aktivitet.objekt",
                    () -> aktivitetDAO.upsertAktivitet(aktivitet),
                    (timer, hasFailed) -> {
                        if (hasFailed) {
                            timer.addTagToReport("aktoerhash", DigestUtils.md5Hex(aktivitet.getAktorId()).toUpperCase());
                        }
                    }
            );
        } catch (Exception e) {
            log.error("Kunne ikke lagre aktivitetdata fra feed for aktivitetid {}.", aktivitet.getAktivitetId(), e);
        }
    }

    private void behandleAktivitetdata(List<AktoerId> aktoerids) {
        try {
            timed(
                    "feed.aktivitet.indekseraktivitet",
                    () -> {
                        Map<AktoerId, Optional<PersonId>> aktoeridToPersonid = aktoerService.hentPersonidsForAktoerids(aktoerids);
                        List<PersonId> personIds = aktoeridToPersonid.values().stream()
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .collect(toList());

                        Map<PersonId, Oppfolgingstatus> oppfolgingstatus = brukerRepository.retrieveOppfolgingstatus(personIds)
                                .getOrElseThrow(() -> new InternalServerErrorException("Kunne ikke finne oppfolgingsstatus for liste av brukere"));

                        Map<Tuple2<AktoerId,PersonId>, Boolean> aktoerErUndeOppfolging = aktoerids.stream()
                                .filter(a -> aktoeridToPersonid.get(a).isPresent())
                                .collect(toMap(
                                        a -> Tuple.of(a, aktoeridToPersonid.get(a).get()),
                                        a -> erBrukerUnderOppfolging(oppfolgingstatus.get(aktoeridToPersonid.get(a).get()))));

                        List<PersonId> ikkeUnderOppfolging = new ArrayList<>();
                        List<AktoerId> underOppfolging = new ArrayList<>();
                        aktoerErUndeOppfolging.forEach((key, value) -> {
                            if (value) {
                                underOppfolging.add(key._1);
                            } else {
                                ikkeUnderOppfolging.add(key._2);
                            }
                        });

                        aktivitetService.utledOgIndekserAktivitetstatuserForAktoerid(underOppfolging);

                        solrService.slettBrukere(ikkeUnderOppfolging);
                        solrService.commit();
                    },(timer, hasFailed) -> timer.addTagToReport("antall", Integer.toString(aktoerids.size()))

            );
        } catch (Exception e) {
            log.error("Feil ved behandling av aktivitetdata fra feed", e);
        }
    }
}