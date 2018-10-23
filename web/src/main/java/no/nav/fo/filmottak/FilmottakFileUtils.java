package no.nav.fo.filmottak;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.filmottak.FilmottakConfig.SftpConfig;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.TiltakOgAktiviteterForBrukere;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

@Slf4j
public class FilmottakFileUtils {

    public static Try<InputStream> lesYtelsesFil(File file) {
        return Try.of(() -> {
            final byte[] bytes = Files.readAllBytes(file.toPath());
            return new ByteArrayInputStream(bytes);
        });
    }

    public static Try<FileObject> hentFil(SftpConfig sftpConfig) throws FileSystemException {
        FileSystemOptions fsOptions = new FileSystemOptions();
        SftpFileSystemConfigBuilder sftpFileSystemConfigBuilder = SftpFileSystemConfigBuilder.getInstance();
        sftpFileSystemConfigBuilder.setPreferredAuthentications(fsOptions, "password");
        sftpFileSystemConfigBuilder.setStrictHostKeyChecking(fsOptions, "no");

        DefaultFileSystemConfigBuilder defaultFileSystemConfigBuilder = DefaultFileSystemConfigBuilder.getInstance();
        defaultFileSystemConfigBuilder.setUserAuthenticator(fsOptions, new StaticUserAuthenticator("", sftpConfig.getUsername(), sftpConfig.getPassword()));

        FileSystemManager fsManager = VFS.getManager();
        return Try.of(() -> fsManager.resolveFile(sftpConfig.getUrl(), fsOptions));
    }

    public static Try<TiltakOgAktiviteterForBrukere> unmarshallTiltakFil(FileObject fileObject) {
        log.info("Starter unmarshalling av tiltaksfil");
        return Try.of(() -> {
            JAXBContext jaxb = JAXBContext.newInstance("no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1");
            Unmarshaller unmarshaller = jaxb.createUnmarshaller();
            StreamSource source = new StreamSource(fileObject.getContent().getInputStream());
            JAXBElement<TiltakOgAktiviteterForBrukere> jaxbElement = unmarshaller.unmarshal(source, TiltakOgAktiviteterForBrukere.class);
            log.info("Unmarshalling av tiltaksfil ferdig!");
            return jaxbElement.getValue();
        });
    }
}
