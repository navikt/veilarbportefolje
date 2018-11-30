package no.nav.fo.veilarbportefolje.database;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import no.nav.fo.veilarbportefolje.aktivitet.AktivitetDAO;
import no.nav.fo.veilarbportefolje.domene.AktivitetStatus;
import no.nav.fo.veilarbportefolje.domene.BrukerOppdatering;
import no.nav.fo.veilarbportefolje.domene.Brukerdata;
import no.nav.fo.veilarbportefolje.domene.PersonId;
import no.nav.fo.veilarbportefolje.indeksering.IndekseringService;
import no.nav.fo.veilarbportefolje.util.MetricsUtils;

import javax.inject.Inject;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class PersistentOppdatering {

    @Inject
    private IndekseringService indekseringService;

    @Inject
    private BrukerRepository brukerRepository;

    @Inject
    private AktivitetDAO aktivitetDAO;

    public void lagreBrukeroppdateringerIDBogIndekser(List<? extends BrukerOppdatering> brukerOppdateringer) {
        lagreBrukeroppdateringerIDB(brukerOppdateringer);
        List<PersonId> personIds = brukerOppdateringer.stream().map(BrukerOppdatering::getPersonid).map(PersonId::of).collect(toList());
        indekseringService.indekserBrukere(personIds);
    }

    public void lagreBrukeroppdateringerIDB(List<? extends BrukerOppdatering> brukerOppdatering) {
        io.vavr.collection.List.ofAll(brukerOppdatering)
                .sliding(1000, 1000)
                .forEach(
                        MetricsUtils.timed("brukeroppdatering.upsert1000", (oppdateringer) -> {
                            List<? extends BrukerOppdatering> javaList = oppdateringer.toJavaList();

                            List<AktivitetStatus> aktivitetStatuser = javaList
                                    .stream()
                                    .map(BrukerOppdatering::getAktiviteter)
                                    .filter(Objects::nonNull)
                                    .flatMap(Collection::stream)
                                    .collect(toList());

                            lagreBrukerdata(javaList);
                            lagreAktivitetstatuser(aktivitetStatuser);
                        }));
    }

    private void lagreBrukerdata(List<? extends BrukerOppdatering> oppdateringer) {
        MetricsUtils.timed("brukerdata.upsert1000", () -> {
            Map<String, Brukerdata> brukerdata = brukerRepository.retrieveBrukerdata(oppdateringer
                    .stream()
                    .map(BrukerOppdatering::getPersonid)
                    .collect(toList())
            )
                    .stream()
                    .collect(toMap(Brukerdata::getPersonid, Function.identity()));

            List<Brukerdata> brukere = oppdateringer.stream().map((oppdatering) -> {
                Brukerdata bruker = brukerdata.getOrDefault(
                        oppdatering.getPersonid(),
                        new Brukerdata().setPersonid(oppdatering.getPersonid())
                );

                return oppdatering.applyTo(bruker);
            }).collect(toList());

            brukerRepository.insertOrUpdateBrukerdata(brukere, brukerdata.keySet());
            return null;
        });
    }

    void lagreAktivitetstatuser(List<AktivitetStatus> aktivitetStatuser) {
        io.vavr.collection.List.ofAll(aktivitetStatuser)
                .sliding(1000, 1000)
                .forEach(MetricsUtils.timed("aktivitetstatus.upsert1000", (statuserBatch) -> {

                    List<AktivitetStatus> statuserBatchJavaList = statuserBatch.toJavaList();

                    Set<PersonId> personIds = statuserBatchJavaList.stream().map(AktivitetStatus::getPersonid).collect(Collectors.toSet());

                    List<AktivitetStatus> aktivitetstatuserIDb = new ArrayList<>();

                    aktivitetDAO.getAktivitetstatusForBrukere(personIds)
                            .forEach((key, value) -> aktivitetstatuserIDb.addAll(value));

                    List<Tuple2<PersonId, String>> finnesIDb = aktivitetstatuserIDb
                            .stream()
                            .map((status) -> Tuple.of(status.getPersonid(), status.getAktivitetType()))
                            .collect(toList());

                    aktivitetDAO.insertOrUpdateAktivitetStatus(statuserBatchJavaList, finnesIDb);
                }));
    }
}
