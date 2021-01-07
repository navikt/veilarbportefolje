package no.nav.pto.veilarbportefolje.database;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.extern.slf4j.Slf4j;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatering;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
@Slf4j
public class PersistentOppdatering {
    private final ElasticIndexer elasticIndexer;
    private final BrukerRepository brukerRepository;
    private final AktivitetDAO aktivitetDAO;

    @Autowired
    public PersistentOppdatering(ElasticIndexer elasticIndexer, BrukerRepository brukerRepository, AktivitetDAO aktivitetDAO){
        this.elasticIndexer = elasticIndexer;
        this.brukerRepository = brukerRepository;
        this.aktivitetDAO = aktivitetDAO;
    }


    public void lagreBrukeroppdateringerIDBogIndekser(List<? extends BrukerOppdatering> brukerOppdateringer) {
        lagreBrukeroppdateringerIDB(brukerOppdateringer);
        // List<PersonId> personIds = brukerOppdateringer.stream().map(BrukerOppdatering::getPersonid).map(PersonId::of).collect(toList());
        // elasticIndexer.indekser(personIds);
    }

    public void lagreBrukeroppdateringerIDB(List<? extends BrukerOppdatering> brukerOppdatering) {
        io.vavr.collection.List.ofAll(brukerOppdatering)
                .sliding(1000, 1000)
                .forEach(
                        (oppdateringer) -> {
                            List<? extends BrukerOppdatering> javaList = oppdateringer.toJavaList();

                            List<AktivitetStatus> aktivitetStatuser = javaList
                                    .stream()
                                    .map(BrukerOppdatering::getAktiviteter)
                                    .filter(Objects::nonNull)
                                    .flatMap(Collection::stream)
                                    .collect(toList());

                            lagreBrukerdata(javaList);
                            lagreAktivitetstatuser(aktivitetStatuser);
                        });
    }

    private void lagreBrukerdata(List<? extends BrukerOppdatering> oppdateringer) {
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
    }

    void lagreAktivitetstatuser(List<AktivitetStatus> aktivitetStatuser) {
        io.vavr.collection.List.ofAll(aktivitetStatuser)
                .sliding(1000, 1000)
                .forEach((statuserBatch) -> {

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
                });
    }
}
