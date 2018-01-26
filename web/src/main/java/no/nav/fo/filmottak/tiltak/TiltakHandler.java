package no.nav.fo.filmottak.tiltak;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.aktivitet.AktivitetDAO;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.*;
import no.nav.fo.filmottak.FilmottakFileUtils;
import no.nav.fo.service.AktoerService;
import no.nav.fo.service.SolrServiceImpl;
import no.nav.fo.util.AktivitetUtils;
import no.nav.fo.util.MetricsUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Bruker;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.TiltakOgAktiviteterForBrukere;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Tiltaksaktivitet;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import static no.nav.fo.filmottak.tiltak.TiltakUtils.*;
import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.log;

@Slf4j
public class TiltakHandler {

    @Value("${filmottak.tiltak.sftp.URI}")
    private String URI;

    @Value("${environment.name}")
    private String miljo;

    @Value("${veilarbportefolje.filmottak.sftp.login.username}")
    private String filmottakBrukernavn;

    @Value("${veilarbportefolje.filmottak.sftp.login.password}")
    private String filmottakPassord;

    private final TiltakRepository tiltakrepository;
    private final AktivitetDAO aktivitetDAO;
    private final BrukerRepository brukerRepository;
    private final AktoerService aktoerService;

    private boolean kjorer;

    @Inject
    public TiltakHandler(TiltakRepository tiltakRepository, AktivitetDAO aktivitetDAO, AktoerService aktoerService, BrukerRepository brukerRepository) {
        this.aktoerService = aktoerService;
        this.tiltakrepository = tiltakRepository;
        this.aktivitetDAO = aktivitetDAO;
        this.kjorer = false;
        this.brukerRepository = brukerRepository;
    }

    public static Timestamp getDatoFilter() {
        return AktivitetUtils.parseDato(System.getProperty(SolrServiceImpl.DATOFILTER_PROPERTY));
    }

    public void startOppdateringAvTiltakIDatabasen() {
        log.info("Forsøker å starte oppdatering av tiltaksaktiviteter.");
        if (this.kjorer()) {
            log.info("Kunne ikke starte ny oppdatering av tiltak fordi den allerede er midt i en oppdatering");
            return;
        }
        this.kjorer = true;
        hentTiltakOgPopulerDatabase();
    }

    private boolean kjorer() {
        return this.kjorer;
    }

    private void hentTiltakOgPopulerDatabase() {
        Consumer<Throwable> stopped = (t) -> this.kjorer = false;
        log.info("Starter oppdatering av tiltak fra Arena..");
        timed("GR202.hentfil", this::hentFil)
                .onFailure(log(log, "Kunne ikke hente tiltaksfil").andThen(stopped))
                .flatMap(timed("GR202.unmarshall", FilmottakFileUtils::unmarshallTiltakFil))
                .onFailure(log(log, "Kunne ikke unmarshalle tiltaksfilen").andThen(stopped))
                .andThen(timed("GR202.populatedb", this::populerDatabase))
                .onFailure(log(log, "Kunne ikke populere database").andThen(stopped))
                .andThen(() -> this.kjorer = false);
    }

    private void populerDatabase(TiltakOgAktiviteterForBrukere tiltakOgAktiviteterForBrukere) {

        log.info("Starter populering av database");

        tiltakrepository.slettBrukertiltak();
        tiltakrepository.slettEnhettiltak();
        tiltakrepository.slettTiltakskoder();
        aktivitetDAO.slettAlleAktivitetstatus(tiltak);
        aktivitetDAO.slettAlleAktivitetstatus(gruppeaktivitet);
        aktivitetDAO.slettAlleAktivitetstatus(utdanningaktivitet);


        List<Tiltakkodeverk> tiltakkoder =
                tiltakOgAktiviteterForBrukere.getTiltakskodeListe().stream().map(Tiltakkodeverk::of).collect(toList());

        tiltakrepository.lagreTiltakskoder(tiltakkoder);

        MetricsUtils.timed("tiltak.indert.nyesteutlopte", () -> {
            utledOgLagreBrukerData(tiltakOgAktiviteterForBrukere.getBrukerListe());
        });

        MetricsUtils.timed("tiltak.insert.brukertiltak", () -> {
            List<Brukertiltak> brukertiltak = tiltakOgAktiviteterForBrukere.getBrukerListe().stream()
                    .map(Brukertiltak::of)
                    .flatMap(Collection::stream)
                    .collect(toList());

            tiltakrepository.lagreBrukertiltak(brukertiltak);
        });

        MetricsUtils.timed("tiltak.insert.as.aktivitet", () -> {
            utledOgLagreAktivitetstatusForTiltak(tiltakOgAktiviteterForBrukere.getBrukerListe());
        });

        MetricsUtils.timed("tiltak.insert.gruppeaktiviteter", () -> {
            utledOgLagreGruppeaktiviteter(tiltakOgAktiviteterForBrukere.getBrukerListe());
        });

        MetricsUtils.timed("tiltak.insert.gruppeaktiviteter", () -> {
            utledOgLagreUtdanningsaktiviteter(tiltakOgAktiviteterForBrukere.getBrukerListe());
        });

        MetricsUtils.timed("tiltak.insert.enhettiltak", () -> {
            utledOgLagreEnhetTiltak(tiltakOgAktiviteterForBrukere.getBrukerListe());
        });

        log.info("Ferdige med å populere database");
    }

    private void utledOgLagreBrukerData(List<Bruker> brukere) {
        io.vavr.collection.List.ofAll(brukere).sliding(1000, 1000)
                .forEach(brukereBatch -> {

                    List<Fnr> fnrs = brukereBatch.toJavaStream().map(Bruker::getPersonident).map(Fnr::of).collect(toList());
                    Map<Fnr, Optional<PersonId>> personidsMap = aktoerService.hentPersonidsForFnrs(fnrs);
                    List<PersonId> personIds = personidsMap.values().stream()
                            .filter(Optional::isPresent)
                            .map(Optional::get).collect(toList());

                    List<Brukerdata> brukerdata = brukerRepository.retrieveBrukerdata(
                            personIds.stream().map(PersonId::toString).collect(toList()));

                    Map<PersonId, Optional<Brukerdata>> brukerdataMap = toBrukerdataOptionalMap(personIds, brukerdata);

                    List<Brukerdata> brukerdataSomIkkeFinnesIDb = lagListeMedBrukereSomIkkeFinnesIDb(personIds, brukerdataMap);

                    Map<PersonId, TiltakOppdateringer> tiltakOppdateringerFraTiltaksfil =
                            finnTiltaksOppdateringerFraTiltaksfil(brukereBatch, personidsMap);

                    List<Brukerdata> brukereMedOppdatertUtlopsdato = concat(brukerdata.stream(), brukerdataSomIkkeFinnesIDb.stream())
                            .map(b -> oppdaterBrukerDataOmNodvendig(b, tiltakOppdateringerFraTiltaksfil))
                            .collect(toList());

                    List<String> finnesIDb = brukerdata.stream().map(Brukerdata::getPersonid).collect(toList());

                    brukerRepository.insertOrUpdateBrukerdata(brukereMedOppdatertUtlopsdato, finnesIDb);
                });
    }

    private Map<PersonId, Optional<Brukerdata>> toBrukerdataOptionalMap(List<PersonId> personIds, List<Brukerdata> brukerdata) {
        return personIds.stream()
                .collect(toMap(
                        Function.identity(),
                        p -> getBrukerdataForPersonId(brukerdata, p)
                ));
    }

    private Map<PersonId, TiltakOppdateringer> finnTiltaksOppdateringerFraTiltaksfil(io.vavr.collection.List<Bruker> brukereBatch,
                                                                                     Map<Fnr, Optional<PersonId>> personidsMap) {
        return brukereBatch.toJavaStream()
                .filter(bruker -> personidsMap.get(Fnr.of(bruker.getPersonident())).isPresent())
                .collect(toMap(
                        bruker -> personidsMap.get(Fnr.of(bruker.getPersonident())).get(),
                        TiltakUtils::finnOppdateringForBruker
                ));
    }

    private List<Brukerdata> lagListeMedBrukereSomIkkeFinnesIDb(List<PersonId> personIds, Map<PersonId, Optional<Brukerdata>> brukerdataMap) {
        return personIds.stream()
                .filter(personId -> !brukerdataMap.get(personId).isPresent())
                .map(personId -> new Brukerdata().setPersonid(personId.toString()))
                .collect(toList());
    }

    static Brukerdata oppdaterBrukerDataOmNodvendig(Brukerdata brukerdata,
                                                    Map<PersonId, TiltakOppdateringer> tiltakOppdateringerFraTiltaksfil) {
        PersonId personId = PersonId.of(brukerdata.getPersonid());
        TiltakOppdateringer oppdateringer = tiltakOppdateringerFraTiltaksfil.get(personId);

        Set<Timestamp> startDatoer = Stream.of(
                Optional.ofNullable(oppdateringer.getAktivitetStart()),
                Optional.ofNullable(oppdateringer.getNesteAktivitetStart()),
                Optional.ofNullable(brukerdata.getAktivitetStart()),
                Optional.ofNullable(brukerdata.getNesteAktivitetStart())
        )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Iterator<Timestamp> iterator = startDatoer.iterator();
        Timestamp aktivitetStart = Try.of(iterator::next).getOrElse(() -> null);
        Timestamp nesteAktivitetStart = Try.of(iterator::next).getOrElse(() -> null);


        brukerdata.setAktivitetStart(aktivitetStart);
        brukerdata.setNesteAktivitetStart(nesteAktivitetStart);

        Optional.ofNullable(oppdateringer.getForrigeAktivitetStart())
                .ifPresent(kanskjeNyDato ->
                        oppdaterMedNyesteDatofelt(
                                brukerdata::getForrigeAktivitetStart,
                                brukerdata::setForrigeAktivitetStart,
                                kanskjeNyDato));
        Optional.ofNullable(oppdateringer.getNyesteUtlopteAktivitet())
                .ifPresent(kanskjeNyDato ->
                        oppdaterMedNyesteDatofelt(brukerdata::getNyesteUtlopteAktivitet,
                                brukerdata::setNyesteUtlopteAktivitet,
                                kanskjeNyDato));
        return brukerdata;
    }

    private static void oppdaterMedNyesteDatofelt(Supplier<Timestamp> getDato, Consumer<Timestamp> setDate, Timestamp kanskjeNydato) {
        if (getDato.get() == null) {
            setDate.accept(kanskjeNydato);
        } else {
            setDate.accept(nyeste(getDato.get(), kanskjeNydato));
        }
    }


    private static Timestamp nyeste(Timestamp t1, Timestamp t2) {
        return t1.before(t2) ? t2 : t1;
    }

    private Optional<Brukerdata> getBrukerdataForPersonId(List<Brukerdata> brukerdata, PersonId personId) {
        return brukerdata.stream().filter(b -> b.getPersonid().equals(personId.toString())).findFirst();
    }

    private void utledOgLagreEnhetTiltak(List<Bruker> brukere) {
        Map<String, Bruker> fnrTilBruker = new HashMap<>();
        brukere.forEach(bruker -> fnrTilBruker.put(bruker.getPersonident(), bruker));

        List<TiltakForEnhet> tiltakForEnhet = tiltakrepository.hentEnhetTilFodselsnummereMap().entrySet().stream()
                .flatMap(entrySet -> entrySet.getValue().stream()
                        .filter(fnr -> fnrTilBruker.get(fnr) != null)
                        .flatMap(fnr -> fnrTilBruker.get(fnr).getTiltaksaktivitetListe().stream()
                                .map(Tiltaksaktivitet::getTiltakstype)
                                .map(tiltak -> TiltakForEnhet.of(entrySet.getKey(), tiltak))
                        ))
                .distinct()
                .collect(toList());
        tiltakrepository.lagreEnhettiltak(tiltakForEnhet);
    }

    private void utledOgLagreAktivitetstatusForTiltak(List<Bruker> brukere) {
        io.vavr.collection.List.ofAll(brukere)
                .sliding(1000, 1000)
                .forEach((brukereSubList) -> {
                    List<Bruker> brukereJavaBatch = brukereSubList.toJavaList();
                    List<Fnr> fnrs = brukereJavaBatch.stream().map(Bruker::getPersonident).filter(Objects::nonNull).map(Fnr::new).collect(toList());
                    Map<Fnr, Optional<PersonId>> fnrPersonidMap = aktoerService.hentPersonidsForFnrs(fnrs);
                    List<AktivitetStatus> aktivitetStatuses = brukereJavaBatch
                            .stream()
                            .map(bruker -> {
                                Optional<PersonId> personId = fnrPersonidMap.get(new Fnr(bruker.getPersonident()));
                                return personId.map(p -> utledAktivitetstatusForTiltak(bruker, p)).orElse(null);
                            })
                            .filter(Objects::nonNull)
                            .collect(toList());
                    aktivitetDAO.insertAktivitetstatuser(aktivitetStatuses);
                });
    }


    private void utledOgLagreGruppeaktiviteter(List<Bruker> brukere) {
        io.vavr.collection.List.ofAll(brukere)
                .sliding(1000, 1000)
                .forEach((brukereSubList) -> {
                    List<Bruker> brukereJavaBatch = brukereSubList.toJavaList();
                    List<Fnr> fnrs = brukereJavaBatch.stream().map(Bruker::getPersonident).filter(Objects::nonNull).map(Fnr::new).collect(toList());
                    Map<Fnr, Optional<PersonId>> fnrPersonidMap = aktoerService.hentPersonidsForFnrs(fnrs);
                    List<AktivitetStatus> aktivitetStatuses = brukereJavaBatch
                            .stream()
                            .map(bruker -> {
                                Optional<PersonId> personId = fnrPersonidMap.get(new Fnr(bruker.getPersonident()));
                                return personId.map(p -> utledGruppeaktivitetstatus(bruker, p)).orElse(null);
                            })
                            .filter(Objects::nonNull)
                            .collect(toList());
                    aktivitetDAO.insertAktivitetstatuser(aktivitetStatuses);

                });
    }

    private void utledOgLagreUtdanningsaktiviteter(List<Bruker> brukere) {
        io.vavr.collection.List.ofAll(brukere)
                .sliding(1000, 1000)
                .forEach((brukereSubList) -> {
                    List<Bruker> brukereJavaBatch = brukereSubList.toJavaList();
                    List<Fnr> fnrs = brukereJavaBatch.stream().map(Bruker::getPersonident).filter(Objects::nonNull).map(Fnr::new).collect(toList());
                    Map<Fnr, Optional<PersonId>> fnrPersonidMap = aktoerService.hentPersonidsForFnrs(fnrs);
                    List<AktivitetStatus> aktivitetStatuses = brukereJavaBatch
                            .stream()
                            .map(bruker -> {
                                Optional<PersonId> personId = fnrPersonidMap.get(new Fnr(bruker.getPersonident()));
                                return personId.map(p -> TiltakUtils.utledUtdanningsaktivitetstatus(bruker, p)).orElse(null);
                            })
                            .filter(Objects::nonNull)
                            .collect(toList());

                    aktivitetDAO.insertAktivitetstatuser(aktivitetStatuses);

                });
    }

    private Try<FileObject> hentFil() {
        log.info("Starter henting av tiltaksfil");
        try {
            String komplettURI = this.URI.replace("<miljo>", this.miljo).replace("<brukernavn>", this.filmottakBrukernavn).replace("<passord>", filmottakPassord);
            return FilmottakFileUtils.hentTiltakFil(komplettURI);
        } catch (FileSystemException e) {
            log.info("Henting av tiltaksfil feilet");
            return Try.failure(e);
        } finally {
            log.info("Henting av tiltaksfil ferdig!");
        }
    }
}
