package no.nav.fo.filmottak.tiltak;

import io.vavr.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.AktivitetStatus;
import no.nav.fo.domene.Fnr;
import no.nav.fo.domene.PersonId;
import no.nav.fo.filmottak.FileUtils;
import no.nav.fo.service.AktoerService;
import no.nav.fo.util.MetricsUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Bruker;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.TiltakOgAktiviteterForBrukere;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.util.*;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;
import static no.nav.fo.filmottak.tiltak.TiltakUtils.*;
import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.log;


public class TiltakHandler {
    private static Logger logger = LoggerFactory.getLogger(TiltakHandler.class);

    @Value("${tiltak.sftp.URI}")
    private String URI;

    @Value("${environment.name}")
    private String miljo;

    @Value("${veilarbportefolje.filmottak.sftp.login.username}")
    private String filmottakBrukernavn;

    @Value("${veilarbportefolje.filmottak.sftp.login.password}")
    private String filmottakPassord;

    private final TiltakRepository tiltakrepository;
    private final BrukerRepository brukerRepository;
    private final AktoerService aktoerService;

    private boolean isRunning;

    @Inject
    public TiltakHandler(TiltakRepository tiltakRepository, BrukerRepository brukerRepository, AktoerService aktoerService) {
        this.aktoerService = aktoerService;
        this.tiltakrepository = tiltakRepository;
        this.brukerRepository = brukerRepository;
        this.isRunning = false;
    }

    public void startOppdateringAvTiltakIDatabasen() {
        if(this.isRunning()) {
            logger.info("Kunne ikke starte ny oppdatering av tiltak fordi den allerede er midt i en oppdatering");
            return;
        }
        this.isRunning = true;
        hentTiltakOgPopulerDatabase();
    }

    private boolean isRunning() {
        return this.isRunning;
    }

    private void hentTiltakOgPopulerDatabase() {
        Consumer<Throwable> stopped = (t) -> this.isRunning = false;
        logger.info("Starter oppdatering av tiltak fra Arena..");
        timed("GR202.hentfil", this::hentFil)
                .onFailure(log(logger, "Kunne ikke hente tiltaksfil").andThen(stopped))
                .flatMap(timed("GR202.unmarshall", this::unmarshall))
                .onFailure(log(logger, "Kunne ikke unmarshalle tiltaksfilen").andThen(stopped))
                .andThen(timed("GR202.populatedb", this::populerDatabase))
                .onFailure(log(logger, "Kunne ikke populere database").andThen(stopped))
                .andThen(() -> this.isRunning = false);
    }

    private void populerDatabase(TiltakOgAktiviteterForBrukere tiltakOgAktiviteterForBrukere) {

        logger.info("Starter populering av database");

        tiltakrepository.slettBrukertiltak();
        tiltakrepository.slettEnhettiltak();
        tiltakrepository.slettTiltakskoder();
        brukerRepository.slettAlleAktivitetstatus(tiltak);
        brukerRepository.slettAlleAktivitetstatus(gruppeaktivitet);

        Set<String> tiltakskoder = new HashSet<>();
        Set<String> brukerTiltak = new HashSet<>();
        Set<String> enhetTiltak = new HashSet<>();

        tiltakOgAktiviteterForBrukere.getTiltakskodeListe().forEach(tiltakskode -> {
            tiltakrepository.insertTiltakskoder(tiltakskode);
            tiltakskoder.add(tiltakskode.getValue());
        });

        MetricsUtils.timed("tiltak.insert.alle", () -> {
            tiltakOgAktiviteterForBrukere.getBrukerListe().forEach(bruker -> {
                tiltakrepository.insertBrukertiltak(bruker, brukerTiltak);
            });
            return null;
        });

        MetricsUtils.timed("tiltak.insert.as.aktivitet", () -> {
            tiltakOgAktiviteterForBrukere.getBrukerListe().forEach(this::utledOgLagreAktivitetstatusForTiltak);
            return null;
        });

        MetricsUtils.timed("tiltak.insert.gruppeaktiviteter", () -> {
            tiltakOgAktiviteterForBrukere.getBrukerListe().forEach(this::utledOgLagreGruppeaktiviteter);
            return null;
        });


        Map<String, Bruker> personIdTilBruker = new HashMap<>();
        tiltakOgAktiviteterForBrukere.getBrukerListe().forEach(bruker -> personIdTilBruker.put(bruker.getPersonident(), bruker));

        List<TiltakForEnhet> tiltakForEnhet = tiltakrepository.getEnhetMedPersonIder().entrySet().stream()
                .flatMap(entrySet -> entrySet.getValue().stream()
                        .filter(personId -> personIdTilBruker.get(personId) != null)
                        .flatMap(personId -> personIdTilBruker.get(personId).getTiltaksaktivitetListe().stream()
                                .map(tiltaksaktivitet -> {
                                    enhetTiltak.add(tiltaksaktivitet.getTiltakstype());
                                    return tiltaksaktivitet.getTiltakstype();
                                })
                                .map(tiltak -> new TiltakForEnhet(entrySet.getKey(), tiltak))
                        ))
                .distinct()
                .collect(toList());
        tiltakrepository.insertEnhettiltak(tiltakForEnhet);

        logger.info("Ferdige med å populere database");


        logSet(tiltakskoder, "KODEVERK");
        logSet(brukerTiltak, "BRUKERTILTAK");
        logSet(enhetTiltak, "ENHETTILTAK");

    }

    private void utledOgLagreAktivitetstatusForTiltak(Bruker bruker) {
        PersonId personId = personIdErElseNull(new Fnr(bruker.getPersonident()));
        if(Objects.isNull(personId)) {
            return;
        }
        AktivitetStatus aktivitetStatus = utledAktivitetstatusForTiltak(bruker, personId);
        if(Objects.nonNull(aktivitetStatus)){
            brukerRepository.insertAktivitetStatus(aktivitetStatus);
        }
    }

    private void utledOgLagreGruppeaktiviteter(Bruker bruker) {
        PersonId personId = personIdErElseNull(new Fnr(bruker.getPersonident()));
        if(Objects.isNull(personId)) {
            return;
        }
        AktivitetStatus aktivitetStatus = utledGruppeaktivitetstatus(bruker, personId);
        if(Objects.nonNull(aktivitetStatus)){
            brukerRepository.insertAktivitetStatus(aktivitetStatus);
        }
    }


    private void logSet(Set<String> strengSet, String navn) {
        StringBuilder tiltakskoderStreng = new StringBuilder(String.format("\n\nUTSKRIFT %s (%d):", navn, strengSet.size()));
        for (String tiltakskode : strengSet) {
            tiltakskoderStreng.append(String.format("\n%s", tiltakskode));
        }
        logger.info(tiltakskoderStreng.toString());
    }

    private PersonId personIdErElseNull(Fnr fnr) {
        return aktoerService
                .hentPersonidFromFnr(fnr)
                .onFailure((t) -> logger.warn("Kunne ikke finne personId for fnr {}", fnr.toString(), t))
                .getOrNull();
    }

    private Try<FileObject> hentFil() {
        logger.info("Starter henting av tiltaksfil");
        try {
            String komplettURI = this.URI.replace("<miljo>", this.miljo).replace("<brukernavn>", this.filmottakBrukernavn).replace("<passord>", filmottakPassord);
            return FileUtils.hentTiltakFil(komplettURI);
        } catch (FileSystemException e) {
            logger.info("Henting av tiltaksfil feilet");
            return Try.failure(e);
        } finally {
            logger.info("Henting av tiltaksfil ferdig!");
        }
    }

    private Try<TiltakOgAktiviteterForBrukere> unmarshall(FileObject fileObject) {
        logger.info("Starter unmarshalling av tiltaksfil");
        return Try.of(() -> {
            JAXBContext jaxb = JAXBContext.newInstance("no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1");
            Unmarshaller unmarshaller = jaxb.createUnmarshaller();
            JAXBElement<TiltakOgAktiviteterForBrukere> jaxbElement = (JAXBElement<TiltakOgAktiviteterForBrukere>) unmarshaller.unmarshal(fileObject.getContent().getInputStream());
            logger.info("Unmarshalling av tiltaksfil ferdig!");
            return jaxbElement.getValue();
        });
    }
}
