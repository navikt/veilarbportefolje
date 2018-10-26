package no.nav.fo.veilarbportefolje.filmottak;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarbportefolje.filmottak.FilmottakConfig.SftpConfig;
import no.nav.melding.virksomhet.loependeytelser.v1.LoependeYtelser;
import no.nav.melding.virksomhet.tiltakogaktiviteterforbrukere.v1.TiltakOgAktiviteterForBrukere;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

@Slf4j
public class FilmottakFileUtils {

    public static Try<FileObject> hentFil(SftpConfig sftpConfig) {
        log.info("Starter henting av fil fra %s", sftpConfig.getUrl());
        try {
            return FilmottakFileUtils.hentFilViaSftp(sftpConfig);
        } catch (FileSystemException e) {
            log.info("Henting av fil fra %s feilet", sftpConfig.getUrl());
            return Try.failure(e);
        } finally {
            log.info("Henting av fil fra %s ferdig!", sftpConfig.getUrl());
        }
    }

    private static Try<FileObject> hentFilViaSftp(SftpConfig sftpConfig) throws FileSystemException {
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
        return Try.of(() -> fileObject.getContent().getInputStream()).flatMap(file -> unmarshallFile(file, TiltakOgAktiviteterForBrukere.class));
    }

    public static Try<LoependeYtelser> unmarshallLoependeYtelserFil(FileObject fileObject) {
        return Try.of(() -> fileObject.getContent().getInputStream()).flatMap(file -> unmarshallFile(file, LoependeYtelser.class));
    }

    static <T> Try<T> unmarshallFile(InputStream is, Class<T> declaredType) {
        log.info("Starter unmarshalling av %s", declaredType.getName());
        return Try.of(() -> {
            JAXBContext jaxb = JAXBContext.newInstance(declaredType.getPackage().getName());
            Unmarshaller unmarshaller = jaxb.createUnmarshaller();
            StreamSource source = new StreamSource(is);
            JAXBElement<T> jaxbElement = unmarshaller.unmarshal(source, declaredType);
            log.info("Unmarshalling av %s ferdig!", declaredType.getName());
            return jaxbElement.getValue();
        });
    }
}
