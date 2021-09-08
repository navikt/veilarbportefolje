package no.nav.pto.veilarbportefolje.database;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetBrukerOppdatering;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetDAO;
import no.nav.pto.veilarbportefolje.aktiviteter.AktivitetStatus;
import no.nav.pto.veilarbportefolje.domene.BrukerOppdatering;
import no.nav.pto.veilarbportefolje.domene.Brukerdata;
import no.nav.pto.veilarbportefolje.domene.value.PersonId;
import no.nav.pto.veilarbportefolje.elastic.ElasticIndexer;
import no.nav.pto.veilarbportefolje.service.BrukerService;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.GR202_PA_KAFKA;
import static no.nav.pto.veilarbportefolje.config.FeatureToggle.erGR202PaKafka;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersistentOppdatering {
    private final ElasticIndexer elasticIndexer;
    private final BrukerRepository brukerRepository;
    private final AktivitetDAO aktivitetDAO;
    private final BrukerService brukerService;
    private final BrukerDataService brukerDataService;
    private final UnleashService unleashService;

    public void lagreBrukeroppdateringerIDBogIndekser(AktivitetBrukerOppdatering brukerOppdateringer, AktorId aktoerId) {
        lagreBrukeroppdateringerIDB(Collections.singletonList(brukerOppdateringer));
        PersonId personId = Optional.ofNullable(brukerOppdateringer.getPersonid()).map(PersonId::of).orElse(null);

        if(unleashService.isEnabled(GR202_PA_KAFKA)){
            brukerDataService.oppdaterAktivitetBrukerData(aktoerId, personId);
        }
        elasticIndexer.indekser(aktoerId);
    }

    public void lagreBrukeroppdateringerIDB(List<? extends BrukerOppdatering> brukerOppdatering) {
        boolean erGr202PaKafka = erGR202PaKafka(unleashService);
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
                            if(!erGr202PaKafka){
                                lagreBrukerdata(javaList);
                            }
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

    public void lagreAktivitetstatuser(List<AktivitetStatus> aktivitetStatuser) {
        io.vavr.collection.List.ofAll(aktivitetStatuser)
                .sliding(1000, 1000)
                .forEach((statuserBatch) -> {

                    List<AktivitetStatus> statuserBatchJavaList = statuserBatch.toJavaList();

                    Set<PersonId> personIds = statuserBatchJavaList.stream().map(AktivitetStatus::getPersonid).collect(Collectors.toSet());

                    List<AktivitetStatus> aktivitetstatuserIDb = new ArrayList<>();

                    aktivitetDAO.getAktivitetstatusForBrukere(personIds)
                            .forEach((key, value) -> aktivitetstatuserIDb.addAll(value));

                    statuserBatchJavaList.forEach(aktivitetStatus -> {
                        if (aktivitetStatus.getAktoerid() == null) {
                            Optional<AktorId> aktorId = brukerService.hentAktorId(aktivitetStatus.getPersonid());
                            aktorId.ifPresent(aktivitetStatus::setAktoerid);
                            log.info("AktivitetStatus: Oppdatert til aktorId {}", aktorId.orElse(null));
                        }
                    });

                    List<Tuple2<PersonId, String>> finnesIDb = aktivitetstatuserIDb
                            .stream()
                            .map((status) -> Tuple.of(status.getPersonid(), status.getAktivitetType()))
                            .collect(toList());

                    aktivitetDAO.insertOrUpdateAktivitetStatus(statuserBatchJavaList, finnesIDb);
                });
    }
}
