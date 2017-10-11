package no.nav.fo.filmottak.tiltak;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.aktivitet.AktivitetDAO;
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

import static java.util.stream.Collectors.toList;
import static no.nav.fo.filmottak.tiltak.TiltakUtils.*;
import static no.nav.fo.util.ListUtils.distinctByPropertyList;
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
    private final AktoerService aktoerService;

    private boolean kjorer;

    private Timestamp datofilter;


    @Inject
    public TiltakHandler(TiltakRepository tiltakRepository, AktivitetDAO aktivitetDAO, AktoerService aktoerService) {
        this.aktoerService = aktoerService;
        this.tiltakrepository = tiltakRepository;
        this.aktivitetDAO = aktivitetDAO;
        this.kjorer = false;
        this.datofilter = AktivitetUtils.parseDato(System.getProperty(SolrServiceImpl.DATOFILTER_PROPERTY));
    }

    public void startOppdateringAvTiltakIDatabasen() {
        log.info("Forsøker å starte oppdatering av tiltaksaktiviteter.");
        if(this.kjorer()) {
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


        List<Tiltakkodeverk> tiltakkoder = distinctByPropertyList(
                Tiltakkodeverk::getKode,
                tiltakOgAktiviteterForBrukere.getTiltakskodeListe().stream().map(Tiltakkodeverk::of).collect(toList()),
                tiltakOgAktiviteterForBrukere.getAktivitetskodeListe().stream().map(Tiltakkodeverk::of).collect(toList())
        );

        tiltakrepository.lagreTiltakskoder(tiltakkoder);

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

        MetricsUtils.timed("tiltak.insert.enhettiltak", () -> {
            utledOgLagreEnhetTiltak(tiltakOgAktiviteterForBrukere.getBrukerListe());
        });

        log.info("Ferdige med å populere database");
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
                .sliding(1000,1000)
                .forEach((brukereSubList) -> {
                    List<Bruker> brukereJavaBatch = brukereSubList.toJavaList();
                    List<Fnr> fnrs = brukereJavaBatch.stream().map(Bruker::getPersonident).filter(Objects::nonNull).map(Fnr::new).collect(toList());
                    Map<Fnr, Optional<PersonId>> fnrPersonidMap = aktoerService.hentPersonidsForFnrs(fnrs);
                    List<AktivitetStatus> aktivitetStatuses = brukereJavaBatch
                            .stream()
                            .map(bruker -> {
                                Optional<PersonId> personId = fnrPersonidMap.get(new Fnr(bruker.getPersonident()));
                                return personId.map(p -> utledAktivitetstatusForTiltak(bruker, p, datofilter)).orElse(null);
                            })
                            .filter(Objects::nonNull)
                            .collect(toList());
                    aktivitetDAO.insertAktivitetstatuser(aktivitetStatuses);
                });
    }


    private void utledOgLagreGruppeaktiviteter(List<Bruker> brukere) {
        io.vavr.collection.List.ofAll(brukere)
                .sliding(1000,1000)
                .forEach((brukereSubList) -> {
                    List<Bruker> brukereJavaBatch = brukereSubList.toJavaList();
                    List<Fnr> fnrs = brukereJavaBatch.stream().map(Bruker::getPersonident).filter(Objects::nonNull).map(Fnr::new).collect(toList());
                    Map<Fnr, Optional<PersonId>> fnrPersonidMap = aktoerService.hentPersonidsForFnrs(fnrs);
                    List<AktivitetStatus> aktivitetStatuses = brukereJavaBatch
                            .stream()
                            .map(bruker -> {
                                Optional<PersonId> personId = fnrPersonidMap.get(new Fnr(bruker.getPersonident()));
                                return personId.map(p -> utledGruppeaktivitetstatus(bruker, p, datofilter)).orElse(null);
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
