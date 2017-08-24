package no.nav.fo.consumer.GR202;

import io.vavr.control.Try;
import no.nav.fo.database.BrukerRepository;
import no.nav.fo.domene.TiltakForEnhet;
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


import java.util.List;
import java.util.stream.Collectors;

import static no.nav.fo.util.MetricsUtils.timed;
import static no.nav.fo.util.StreamUtils.log;


public class KopierGR202FraArena {
    static Logger logger = LoggerFactory.getLogger(KopierGR202FraArena.class);

    @Value("${tiltak.sftp.URI}")
    private String URI;

    private final BrukerRepository brukerRepository;

    @Inject
    public KopierGR202FraArena(BrukerRepository brukerRepository) {
        this.brukerRepository = brukerRepository;
    }

    public void hentTiltaksOgPopulerDatabase() {
        logger.info("Starter oppdatering av tiltak fra Arena..");
        timed("GR202.hentfil", this::hentFil)
                .onFailure(log(logger, "Kunne ikke hente tiltaksfil"))
                .flatMap(timed("GR202.unmarshall", this::unmarshall))
                .onFailure(log(logger, "Kunne ikke unmarshalle tiltaksfilen"))
                .andThen(timed("GR202.populatedb", this::populerDatabase))
                .onFailure(log(logger, "Kunne ikke populere database"));
    }

    private void populerDatabase(TiltakOgAktiviteterForBrukere tiltakOgAktiviteterForBrukere) {

        logger.info("Starter populering database");

        brukerRepository.slettBrukertiltak();
        brukerRepository.slettEnhettiltak();
        brukerRepository.slettTiltakskoder();

        tiltakOgAktiviteterForBrukere.getTiltakskodeListe().forEach(brukerRepository::insertTiltakskoder);
        tiltakOgAktiviteterForBrukere.getBrukerListe().forEach(brukerRepository::insertBrukertiltak);

        List<TiltakForEnhet> tiltakForEnhet = brukerRepository.getEnhetMedPersonIder().entrySet().stream().flatMap(entrySet -> entrySet.getValue().stream()
                .flatMap(personId -> tiltakOgAktiviteterForBrukere.getBrukerListe().stream()
                        .filter(bruker -> bruker.getPersonident().equals(personId))
                        .flatMap(bruker -> bruker.getTiltaksaktivitetListe().stream()
                                .map(Tiltaksaktivitet::getTiltakstype)
                                .map(tiltak -> new TiltakForEnhet(entrySet.getKey(), tiltak))
                        )
                ))
                .distinct()
                .collect(Collectors.toList());
        brukerRepository.insertEnhettiltak(tiltakForEnhet);

        logger.info("Ferdige med å populere database");
    }

    private Try<FileObject> hentFil() {
        logger.info("Starter henting av tiltaksfil");
        FileSystemOptions fsOptions = new FileSystemOptions();
        try {
            SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fsOptions, "no");
            FileSystemManager fsManager = VFS.getManager();
            return Try.of(() -> fsManager.resolveFile(URI, fsOptions));
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
