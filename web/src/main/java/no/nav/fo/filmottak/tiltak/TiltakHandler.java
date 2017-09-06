package no.nav.fo.filmottak.tiltak;

import io.vavr.control.Try;
import no.nav.fo.filmottak.FilmottakFileUtils;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Bruker;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.TiltakOgAktiviteterForBrukere;
import org.apache.commons.vfs2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.log;


public class TiltakHandler {
    private static Logger logger = LoggerFactory.getLogger(TiltakHandler.class);

    @Value("${filmottak.tiltak.sftp.URI}")
    private String URI;

    @Value("${environment.name}")
    private String miljo;

    @Value("${veilarbportefolje.filmottak.sftp.login.username}")
    private String filmottakBrukernavn;

    @Value("${veilarbportefolje.filmottak.sftp.login.password}")
    private String filmottakPassord;

    private final TiltakRepository tiltakrepository;

    private boolean isRunning;

    @Inject
    public TiltakHandler(TiltakRepository tiltakRepository) {
        this.tiltakrepository = tiltakRepository;
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
                .flatMap(timed("GR202.unmarshall", FilmottakFileUtils::unmarshallTiltakFil))
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

        Set<String> tiltakskoder = new HashSet<>();
        Set<String> brukerTiltak = new HashSet<>();
        Set<String> enhetTiltak = new HashSet<>();

        tiltakOgAktiviteterForBrukere.getTiltakskodeListe().forEach(tiltakskode -> {
            tiltakrepository.insertTiltakskoder(tiltakskode);
            tiltakskoder.add(tiltakskode.getValue());
        });

        tiltakOgAktiviteterForBrukere.getBrukerListe().forEach(bruker -> {
            tiltakrepository.insertBrukertiltak(bruker, brukerTiltak);
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
                                .map(tiltak -> TiltakForEnhet.of(entrySet.getKey(), tiltak))
                        ))
                .distinct()
                .collect(Collectors.toList());
        tiltakrepository.insertEnhettiltak(tiltakForEnhet);

        logger.info("Ferdige med å populere database");


        logSet(tiltakskoder, "KODEVERK");
        logSet(brukerTiltak, "BRUKERTILTAK");
        logSet(enhetTiltak, "ENHETTILTAK");

    }

    private void logSet(Set<String> strengSet, String navn) {
        StringBuilder tiltakskoderStreng = new StringBuilder(String.format("\n\nUTSKRIFT %s (%d):", navn, strengSet.size()));
        for (String tiltakskode : strengSet) {
            tiltakskoderStreng.append(String.format("\n%s", tiltakskode));
        }
        logger.info(tiltakskoderStreng.toString());
    }

    private Try<FileObject> hentFil() {
        logger.info("Starter henting av tiltaksfil");
        try {
            String komplettURI = this.URI.replace("<miljo>", this.miljo).replace("<brukernavn>", this.filmottakBrukernavn).replace("<passord>", filmottakPassord);
            return FilmottakFileUtils.hentTiltakFil(komplettURI);
        } catch (FileSystemException e) {
            logger.info("Henting av tiltaksfil feilet");
            return Try.failure(e);
        } finally {
            logger.info("Henting av tiltaksfil ferdig!");
        }
    }
}
