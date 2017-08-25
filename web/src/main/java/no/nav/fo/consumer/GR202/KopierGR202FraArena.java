package no.nav.fo.consumer.GR202;

import io.vavr.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.TiltakForEnhet;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Bruker;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.TiltakOgAktiviteterForBrukere;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.Tiltaksaktivitet;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;


import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.log;


public class KopierGR202FraArena {
    static Logger logger = LoggerFactory.getLogger(KopierGR202FraArena.class);

    @Value("${tiltak.sftp.URI}")
    private String URI;

    @Value("${environment.name}")
    private String miljo;

    @Value("${veilarbportefolje.filmottak.sftp.login.username}")
    private String filmottakBrukernavn;

    @Value("${veilarbportefolje.filmottak.sftp.login.password}")
    private String filmottakPassord;

    private final BrukerRepository brukerRepository;

    private boolean isRunning;

    @Inject
    public KopierGR202FraArena(BrukerRepository brukerRepository) {
        this.brukerRepository = brukerRepository;
        this.isRunning = false;
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public void hentTiltakOgPopulerDatabase() {
        this.isRunning = true;
        Consumer<Throwable> stopped = (t) -> this.isRunning = false;
        logger.info("Starter oppdatering av tiltak fra Arena..");
        timed("GR202.hentfil", this::hentFil)
                .onFailure(log(logger, "Kunne ikke hente tiltaksfil").andThen(stopped))
                .flatMap(timed("GR202.unmarshall", this::unmarshall))
                .onFailure(log(logger, "Kunne ikke unmarshalle tiltaksfilen").andThen(stopped))
                .andThen(timed("GR202.populatedb", this::populerDatabase))
                .onFailure(log(logger, "Kunne ikke populere database").andThen(stopped));
    }

    private void populerDatabase(TiltakOgAktiviteterForBrukere tiltakOgAktiviteterForBrukere) {

        logger.info("Starter populering av database");

        brukerRepository.slettBrukertiltak();
        brukerRepository.slettEnhettiltak();
        brukerRepository.slettTiltakskoder();

        Set<String> tiltakskoder = new HashSet<>();
        Set<String> brukerTiltak = new HashSet<>();
        Set<String> enhetTiltak = new HashSet<>();

        tiltakOgAktiviteterForBrukere.getTiltakskodeListe().forEach(tiltakskode -> {
            brukerRepository.insertTiltakskoder(tiltakskode);
            tiltakskoder.add(tiltakskode.getValue());
        });

        tiltakOgAktiviteterForBrukere.getBrukerListe().forEach(bruker -> {
            brukerRepository.insertBrukertiltak(bruker, brukerTiltak);
        });

        Map<String, Bruker> personIdTilBruker = new HashMap<>();
        tiltakOgAktiviteterForBrukere.getBrukerListe().forEach(bruker -> personIdTilBruker.put(bruker.getPersonident(), bruker));

        List<TiltakForEnhet> tiltakForEnhet = brukerRepository.getEnhetMedPersonIder().entrySet().stream()
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
                .collect(Collectors.toList());
        brukerRepository.insertEnhettiltak(tiltakForEnhet);

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
        FileSystemOptions fsOptions = new FileSystemOptions();
        try {
            SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fsOptions, "no");
            FileSystemManager fsManager = VFS.getManager();
            String komplettURI = this.URI.replace("<miljo>", this.miljo).replace("<brukernavn>", this.filmottakBrukernavn).replace("<passord>", filmottakPassord);
            return Try.of(() -> fsManager.resolveFile(komplettURI, fsOptions));
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
