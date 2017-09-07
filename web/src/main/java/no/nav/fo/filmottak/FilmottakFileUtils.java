package no.nav.fo.filmottak;

import io.vavr.control.Try;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.TiltakOgAktiviteterForBrukere;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class FilmottakFileUtils {

    private static Logger logger = LoggerFactory.getLogger(FilmottakFileUtils.class);

    public static Try<InputStream> lesYtelsesFil(File file) {
        return Try.of(() -> {
            final byte[] bytes = Files.readAllBytes(file.toPath());
            return new ByteArrayInputStream(bytes);
        });
    }

    public static Try<FileObject> hentTiltakFil(String URI) throws FileSystemException {
        FileSystemOptions fsOptions = new FileSystemOptions();
        SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fsOptions, "no");
        FileSystemManager fsManager = VFS.getManager();
        return Try.of(() -> fsManager.resolveFile(URI, fsOptions));
    }

    public static Try<TiltakOgAktiviteterForBrukere> unmarshallTiltakFil(FileObject fileObject) {
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
